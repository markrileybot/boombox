#include <math.h>
#include <stdio.h>
#include <CurieBLE.h>
#include "server.h"
#include "blespp.h"
#include "launch_control.h"

unsigned int loopCounter = 0;
bool ledHi = false;

const byte *server_reply;
unsigned int server_reply_len;

void on_recv(const byte* msg, unsigned int len) {
  Serial.print(" >>> ");
  Serial.println(len);  
  server_receive(msg, len, &server_reply, &server_reply_len);
}

void on_reset() {
  server_reset();
}

void init_launch_control() {
  if (launch_control_init()) {
    Serial.println("Launch control init complete");
  } else {
    Serial.println("Launch control init failed!");
  }
}

void init_server() {
  if (server_init()) {
    Serial.println("Comms init complete");
  } else {
    Serial.println("Comms init failed!");
  }
}

void init_ble() {
  if (blespp_init(on_recv, on_reset)) {
    blespp_open();
    Serial.println("BLE init complete");
  } else {
    Serial.println("BLE init failed!");
  }  
}

void setup() {
  Serial.begin(15200);
  pinMode(LED_BUILTIN, OUTPUT);

  delay(1000);

  init_launch_control();

  delay(1000);
  
  init_server();

  delay(1000);
  
  init_ble();
}

// the loop function runs over and over again forever
void loop() {
  if (!(++loopCounter % 10000)) {
    ledHi = !ledHi;
    digitalWrite(LED_BUILTIN, ledHi ? HIGH : LOW);
  }
  
  bool ble_is_open = blespp_is_open();
  
  if (ble_is_open) {
    blespp_poll();

    if (server_reply_len > 0) {
      blespp_send(server_reply, server_reply_len);
      server_reply = NULL;

      Serial.print(" <<< ");
      Serial.println(server_reply_len);

      server_reply_len = 0;    
    }
  }
  
  if (launch_control_ready()) {
    if (ble_is_open) {
      blespp_close();
    } else {
      launch_control_fire();
    }
  } else if (!ble_is_open) {
    blespp_open();
  }
}



