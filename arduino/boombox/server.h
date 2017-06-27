
#ifndef SERVER_H
#define SERVER_H

#include <stdbool.h>
#include <Arduino.h>
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

bool server_init();

bool server_reset();

bool server_receive(const byte *chunk, unsigned int chunk_len,
  const byte** resp, unsigned int *resp_len);

#ifdef __cplusplus
}
#endif
#endif

