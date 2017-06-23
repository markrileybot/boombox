#include <math.h>
#include <stdio.h>
#include <CurieBLE.h>
#include "server.h"
#include "blespp.h"

unsigned int loopCounter = 0;
bool ledHi = false;

const byte *server_reply;
unsigned int server_reply_len;


void on_send(const byte* msg, unsigned int len) {
  server_reply = msg;
  server_reply_len = len;
}

void on_recv(const byte* msg, unsigned int len) {
  Serial.print(" >>> ");
  Serial.println(len);  
  server_process(msg, len);
}

void on_reset() {
  server_reset();
}

void init_server() {
  if (server_init(on_send)) {
    Serial.println("Comms init complete");
  } else {
    Serial.println("Comms init failed!");
  }
}

void init_ble() {  
  if (blespp_init(on_recv, on_reset)) {
    Serial.println("BLE init complete");
  } else {
    Serial.println("BLE init failed!");
  }  
}

void setup() {
  // our funtion pin
  Serial.begin(15200);
  pinMode(LED_BUILTIN, OUTPUT);

  delay(1000);

  init_ble();
  init_server();
}

// the loop function runs over and over again forever
void loop() {
  if (!(++loopCounter % 10000)) {
    ledHi = !ledHi;
    digitalWrite(LED_BUILTIN, ledHi ? HIGH : LOW);
  }
  if (server_reply_len > 0) {
    const byte *reply = server_reply;
    unsigned int len = server_reply_len;

    server_reply = NULL;
    server_reply_len = 0;

    blespp_send(reply, len);
    server_reply_len = 0;

    Serial.print(" <<< ");
    Serial.println(len);
  }
  BLE.poll();  
}


