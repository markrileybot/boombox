#include <stdint.h>

#ifndef TN_BSWAP_H
#define TN_BSWAP_H

// crappy impls...these should be replaced with arm
// swap instructions
int64_t bswap_64(int64_t x);
int32_t bswap_32(int64_t x);
int16_t bswap_16(int64_t x);
#endif

