
#include <types.h>
#include <transport.h>
#include <mem.h>
#include "types.h"

const static int TN_FT_MAX_CHUNK_SIZE = 256;

static void
tn_transport_base_destroy(tn_object_t *t)
{
    tn_free(t);
}
static bool
tn_transport_base_is_open(tn_transport_t *self)
{
	return true;
}
static size_t
tn_transport_base_read(tn_transport_t *self, void *buf, size_t len, tn_error_t *error)
{
	return 0;
}
static size_t
tn_transport_base_write(tn_transport_t *self, void *buf, size_t len, tn_error_t *error)
{
	return 0;
}
static size_t
tn_transport_base_skip(tn_transport_t *self, size_t len, tn_error_t *error)
{
    return 0;
}
static void
tn_transport_base_reset(tn_object_t *self)
{
}
tn_transport_t*
tn_transport_init(tn_transport_t *self, tn_error_t *error)
{
    self->parent.tn_destroy = &tn_transport_base_destroy;
    self->parent.tn_reset = &tn_transport_base_reset;
	self->tn_is_open = &tn_transport_base_is_open;
	self->tn_read = &tn_transport_base_read;
	self->tn_write = &tn_transport_base_write;
    self->tn_skip = &tn_transport_base_skip;
	return self;
}
tn_transport_t*
tn_transport_create(tn_error_t *error)
{
	tn_transport_t *t = tn_alloc(sizeof(tn_transport_t), error);
	if( *error != 0 ) return NULL;
	return tn_transport_init(t, error);
}
bool
tn_transport_is_open(tn_transport_t *self)
{
    return self->tn_is_open(self);
}
size_t
tn_transport_read(tn_transport_t *self, void *buf, size_t len, tn_error_t *error)
{
    return self->tn_read(self, buf, len, error);
}
size_t
tn_transport_write(tn_transport_t *self, void *buf, size_t len, tn_error_t *error)
{
    return self->tn_write(self, buf, len, error);
}
size_t
tn_transport_skip(tn_transport_t *self, size_t len, tn_error_t *error)
{
    return self->tn_skip(self, len, error);
}
#if THRIFT_TRANSPORT_MEMORY
static void
tn_transport_memory_destroy(tn_object_t *t)
{
    tn_object_destroy(((tn_transport_memory_t*)t)->buf);
    tn_free(t);
}
static size_t
tn_transport_memory_read(tn_transport_t *self, void *buf, size_t len, tn_error_t *error)
{
	tn_transport_memory_t *mem = (tn_transport_memory_t*) self;
	size_t l = tn_buffer_read(mem->buf, buf, len);
    if( l != len ) *error = T_ERR_BUFFER_UNDERFLOW;
    return l;
}
static size_t
tn_transport_memory_write(tn_transport_t *self, void *buf, size_t len, tn_error_t *error)
{
	tn_transport_memory_t *mem = (tn_transport_memory_t*) self;
	size_t l = tn_buffer_write(mem->buf, buf, len);
    if( l != len ) *error = T_ERR_BUFFER_OVERFLOW;
    return l;
}
static size_t
tn_transport_memory_skip(tn_transport_t *self, size_t len, tn_error_t *error)
{
    tn_transport_memory_t *mem = (tn_transport_memory_t*) self;
    size_t l = tn_buffer_skip(mem->buf, len);
    if( l != len ) *error = T_ERR_BUFFER_OVERFLOW;
    return l;
}
static void
tn_transport_memory_reset(tn_object_t *self)
{
	tn_object_reset(((tn_transport_memory_t*)self)->buf);
}
tn_transport_t *
tn_transport_memory_init(tn_transport_memory_t *s, size_t bufferSize, tn_error_t *error)
{
	tn_transport_t *self = (tn_transport_t*) s;
    tn_transport_init(self, error);
    self->parent.tn_destroy = &tn_transport_memory_destroy;
    self->parent.tn_reset = &tn_transport_memory_reset;
    self->tn_read = &tn_transport_memory_read;
    self->tn_write = &tn_transport_memory_write;
    self->tn_skip = &tn_transport_memory_skip;
	if( s->buf == NULL )
	{
		s->buf = tn_buffer_create(bufferSize, error);
	}
	return self;
}
tn_transport_t*
tn_transport_memory_create(size_t bufferSize, tn_error_t *error)
{
	tn_transport_memory_t *t = tn_alloc(sizeof(tn_transport_memory_t), error);
	if( *error != 0 ) return NULL;
	t->buf = NULL;
	return tn_transport_memory_init(t, bufferSize, error);
}
void
tn_transport_memory_rewind(tn_transport_memory_t *self)
{
    self->buf->pos = 0;
}
#endif

#if THRIFT_TRANSPORT_FILE
static void
tn_transport_file_destroy(tn_object_t *t)
{
    tn_transport_file_t *file = (tn_transport_file_t*)t;
    if( file->fd != NULL )
    {
        fclose(file->fd);
        file->fd = NULL;
    }
    tn_free(t);
}
static size_t
tn_transport_file_read(tn_transport_t *self, void *buf, size_t len, tn_error_t *error)
{
    tn_transport_file_t *file = (tn_transport_file_t*) self;
    size_t l = fread(buf, 1, len, file->fd);
    if( l != len ) *error = T_ERR_BUFFER_UNDERFLOW;
    return l;
}
static size_t
tn_transport_file_write(tn_transport_t *self, void *buf, size_t len, tn_error_t *error)
{
    tn_transport_file_t *file = (tn_transport_file_t*) self;
    size_t l = fwrite(buf, 1, len, file->fd);
    if( l != len ) *error = T_ERR_BUFFER_OVERFLOW;
    return l;
}
static size_t
tn_transport_file_skip(tn_transport_t *self, size_t len, tn_error_t *error)
{
    tn_transport_file_t *file = (tn_transport_file_t*) self;
    if( !fseek(file->fd, len, SEEK_CUR) )
    {
        return len;
    }

    // fd might not support seek...try to read chunks
    const char cbuf[TN_FT_MAX_CHUNK_SIZE];
    void *buf = (void*) &cbuf;
    size_t total = 0;
    while( (total += tn_transport_file_read(self, buf, MIN(total-len, TN_FT_MAX_CHUNK_SIZE), error)) < len )
    {
        if( *error != 0 )
        {
            break;
        }
    }

    return total;
}
tn_transport_t *
tn_transport_file_init(tn_transport_file_t *s, FILE *fd, tn_error_t *error)
{
    tn_transport_t *self = (tn_transport_t*) s;
    tn_transport_init(self, error);
    self->parent.tn_destroy = &tn_transport_file_destroy;
    self->tn_read = &tn_transport_file_read;
    self->tn_write = &tn_transport_file_write;
    self->tn_skip = &tn_transport_file_skip;
    s->fd = fd;
    return self;
}
tn_transport_t*
tn_transport_file_create(FILE *fd, tn_error_t *error)
{
    tn_transport_file_t *t = tn_alloc(sizeof(tn_transport_file_t), error);
    if( *error != 0 ) return NULL;
    return tn_transport_file_init(t, fd, error);
}
#endif

#if THRIFT_TRANSPORT_BUFFER
static void
tn_transport_buffer_reset(tn_object_t *self)
{
    tn_transport_buffer_t *buf = (tn_transport_buffer_t*)self;
    buf->buf->pos = 0;
}
static void
tn_transport_buffer_destroy(tn_object_t *t)
{
    tn_transport_buffer_t *buf = (tn_transport_buffer_t*)t;
    if(buf->delegate != NULL) {
        tn_object_destroy(buf->delegate);
        buf->delegate = NULL;
    }
    if(buf->buf) {
        tn_object_destroy(buf->buf);
        buf->buf = NULL;
    }
    tn_free(t);
}
static size_t
tn_transport_buffer_read(tn_transport_t *t, void *buf, size_t len, tn_error_t *error)
{
    tn_transport_buffer_t *self = (tn_transport_buffer_t*)t;
    size_t l = 0, d = 0;
    size_t remaining = self->buf->len - self->buf->pos;
    if( remaining > 0 ) {
        l = tn_buffer_read(self->buf, buf, MIN(remaining, len));
    }
    if(len - l > 0) {
        d = self->delegate->tn_read(self->delegate, buf+l, len-l, error);
        tn_buffer_write(self->buf, buf+l, d);
        l += d;
        if(l < len) {
            *error = EAGAIN;
        }
    }
    return l;
}
static size_t
tn_transport_buffer_skip(tn_transport_t *t, size_t len, tn_error_t *error)
{
    tn_transport_buffer_t *self = (tn_transport_buffer_t*)t;
    size_t l = 0, d = 0;
    size_t remaining = self->buf->len - self->buf->pos;
    if( remaining > 0 ) {
        l = tn_buffer_skip(self->buf, MIN(remaining, len));
    }
    if(len - l > 0) {
        remaining = self->buf->pos;
        d = self->delegate->tn_read(self->delegate, tn_buffer_get(self->buf, len-l), len-l, error);
        l += d;
        if(l < len) {
            self->buf->pos = remaining + d;
            *error = EAGAIN;
        }
    }
    return l;
}
static size_t
tn_transport_buffer_write(tn_transport_t *t, void *buf, size_t len, tn_error_t *error)
{
    tn_transport_buffer_t *self = (tn_transport_buffer_t*)t;
    size_t l = 0;
    self->buf->pos = self->buf->len;
    if(self->buf->pos > 0) {
        l = tn_buffer_write(self->buf, buf, len);
    } else {
        l = self->delegate->tn_write(self->delegate, buf, len, error);
        if(l < len) {
            tn_buffer_write(self->buf, buf+l, len-1);
        }
    }
    self->buf->pos = 0;
    return l;
}
tn_transport_t*
tn_transport_buffer_init(tn_transport_buffer_t *self, tn_transport_t *delegate, tn_error_t *error)
{
    if(self->delegate != NULL) {
        tn_object_destroy(self->delegate);
        self->delegate = NULL;
    }
    self->delegate = delegate;
    if(self->buf == NULL) {
        self->buf = tn_buffer_create(32, error);
    }
    tn_transport_t *p = (tn_transport_t*) self;
    p->parent.tn_destroy = &tn_transport_buffer_destroy;
    p->parent.tn_reset = &tn_transport_buffer_reset;
    p->tn_read = &tn_transport_buffer_read;
    p->tn_write = &tn_transport_buffer_write;
    p->tn_skip = &tn_transport_buffer_skip;
    return p;
}
tn_transport_t*
tn_transport_buffer_create(tn_transport_t *delegate, tn_error_t *error)
{
    tn_transport_buffer_t *t = tn_alloc(sizeof(tn_transport_buffer_t), error);
    if( *error != 0 ) return NULL;
    t->delegate = NULL;
    t->buf = NULL;
    return tn_transport_buffer_init(t, delegate, error);
}
size_t
tn_transport_buffer_flush(tn_transport_t *t, tn_error_t *error)
{
    tn_transport_buffer_t *self = (tn_transport_buffer_t*)t;
    size_t l = 0;
    size_t remaining = self->buf->len - self->buf->pos;
    if(remaining > 0) {
        l = self->delegate->tn_write(self->delegate, self->buf->buf+self->buf->pos, remaining, error);
        self->buf->pos += l;
        if(*error == T_ERR_OK && l < remaining) {
            *error = EAGAIN;
        }
    }
    if(*error == T_ERR_OK) {
        tn_object_reset(self->buf);
        tn_object_reset(self->delegate);
    }
    return l;
}
#endif



