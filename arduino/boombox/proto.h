#ifndef PROTO_H
#define PROTO_H

#ifdef __cplusplus
extern "C" {
#endif

#define BOOMBOX_MAGIC0 11
#define BOOMBOX_MAGIC1 4
#define BOOMBOX_MAGIC2 0
#define BOOMBOX_MAGIC3 4

#define MAGIC_FOOTER_LEN 4

#define OFFSET_SEQ 0
#define OFFSET_TYPE 4
#define OFFSET_MSG 5

/**
 */
typedef enum 
{
  /**
   */
  BOOMBOX_MESSAGE_TYPE_ERROR = 0,
  /**
   */
  BOOMBOX_MESSAGE_TYPE_LAUNCH = 1,
  /**
   */
  BOOMBOX_MESSAGE_TYPE_PING = 2,
  /**
   */
  BOOMBOX_MESSAGE_TYPE_RESET = 3
} message_type_t;

typedef struct
{
  uint16_t delay;
  uint16_t tube;
} sequence_item_t;

typedef struct
{
  uint16_t seq_size;
} launch_t;

#ifdef __cplusplus
}
#endif
#endif

