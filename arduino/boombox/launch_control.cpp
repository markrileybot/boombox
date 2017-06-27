
#include <Arduino.h>
#include <SparkFunSX1509.h>
#include "launch_control.h"

#define PIN_MAX 16

SX1509 relay_io;
launch_t *sequence = NULL;

bool launch_control_init() {
  if(!relay_io.begin(0x3E)) {
    return false;
  }

  for (uint16_t i = 0; i < PIN_MAX; i++) {
    relay_io.pinMode(i, OUTPUT);
  }

  launch_control_reset();

  return true;
}

void launch_control_reset() {
  for (uint16_t i = 0; i < PIN_MAX; i++) {
    relay_io.digitalWrite(i, HIGH);
  }
}

void launch_control_set_sequence(launch_t *cmd) {
  if (sequence == NULL) {
    sequence = cmd;
  }
}

bool launch_control_ready() {
  return sequence != NULL;
}

void launch_control_fire() {
  if (sequence != NULL) {
    sequence_item_t *v = (sequence_item_t*) (sequence + 1);
    uint16_t i;
    
    for (i = 0; i < sequence->seq_size; i++) {
      if (v->interval > 0) {
        delay(v->interval);
      }
      relay_io.digitalWrite(v->tube, LOW);
      v++;
    }
    
    delay(1000);
    
    v = (sequence_item_t*) (sequence + 1);
    for (i = 0; i < sequence->seq_size; i++, v++) {
      relay_io.digitalWrite(v->tube, HIGH);
    }
    sequence = NULL;
  }
}


