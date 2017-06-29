
#define RELAY_TYPE_16CH 16
#define RELAY_TYPE_4CH 4
#define RELAY_TYPE RELAY_TYPE_16CH

#include <Arduino.h>
#if RELAY_TYPE == RELAY_TYPE_16CH
#include <SparkFunSX1509.h>
#endif
#include "launch_control.h"


#if RELAY_TYPE == RELAY_TYPE_4CH
#define PIN_MAX 6
#define PIN_START 2
#else
#define PIN_MAX 16
#define PIN_START 0
SX1509 relay_io;
#endif

launch_t *sequence = NULL;

bool launch_control_init() {
#if RELAY_TYPE == RELAY_TYPE_16CH
  if (!relay_io.begin(0x3E)) {
    return false;
  }
#endif

  for (uint16_t i = PIN_START; i < PIN_MAX; i++) {
#if RELAY_TYPE == RELAY_TYPE_4CH
    pinMode(i, OUTPUT);
#else
    relay_io.pinMode(i, OUTPUT);
#endif
  }

  launch_control_reset();

  return true;
}

void launch_control_reset() {
  for (uint16_t i = PIN_START; i < PIN_MAX; i++) {
#if RELAY_TYPE == RELAY_TYPE_4CH
    digitalWrite(i, LOW);
#else
    relay_io.digitalWrite(i, HIGH);
#endif
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

    for (i = 0; i < sequence->seq_size; i++, v++) {
      if (v->delay > 0) {
        delay(v->delay);
      }
#if RELAY_TYPE == RELAY_TYPE_4CH
      digitalWrite(PIN_START + v->tube, HIGH);
#else
      relay_io.digitalWrite(PIN_START + v->tube, LOW);
#endif
    }

    delay(3000);

    v = (sequence_item_t*) (sequence + 1);
    for (i = 0; i < sequence->seq_size; i++, v++) {
#if RELAY_TYPE == RELAY_TYPE_4CH
      digitalWrite(PIN_START + v->tube, LOW);
#else
      relay_io.digitalWrite(PIN_START + v->tube, HIGH);
#endif
    }
    sequence = NULL;
  }
}


