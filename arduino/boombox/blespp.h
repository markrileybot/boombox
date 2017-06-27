
#ifndef BLESPP_H
#define BLESPP_H

#include <Arduino.h>
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

bool blespp_init(io_handler h1, reset_handler h2);

void blespp_open();

void blespp_close();

bool blespp_is_open();

bool blespp_send(const byte* msg, unsigned int len);

void blespp_poll();

#ifdef __cplusplus
}
#endif
#endif

