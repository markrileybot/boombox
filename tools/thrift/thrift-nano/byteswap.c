#include <byteswap.h>

// crappy impls...these should be replaced with arm
// swap instructions
int64_t bswap_64(int64_t x) {
  int8_t *buf = (int8_t*) &x;
  return  ((int64_t)(buf[0] & 0xff) << 56) |
      ((int64_t)(buf[1] & 0xff) << 48) |
      ((int64_t)(buf[2] & 0xff) << 40) |
      ((int64_t)(buf[3] & 0xff) << 32) |
      ((int64_t)(buf[4] & 0xff) << 24) |
      ((int64_t)(buf[5] & 0xff) << 16) |
      ((int64_t)(buf[6] & 0xff) <<  8) |
      ((int64_t)(buf[7] & 0xff));
}
int32_t bswap_32(int64_t x) {
  int8_t *buf = (int8_t*) &x;
  return  ((int32_t)(buf[0] & 0xff) << 24) |
      ((int32_t)(buf[1] & 0xff) << 16) |
      ((int32_t)(buf[2] & 0xff) <<  8) |
      ((int32_t)(buf[3] & 0xff));
}
int16_t bswap_16(int64_t x) {
  int8_t *buf = (int8_t*) &x;
  return  ((int16_t)(buf[0] & 0xff) <<  8) |
      ((int16_t)(buf[1] & 0xff));
}

