namespace * boombox.proto
namespace c_nano boombox

const byte MAGIC0 = 0xb;
const byte MAGIC1 = 0x4;
const byte MAGIC2 = 0x0;
const byte MAGIC3 = 0x4;

enum MessageType {
	ERROR,
	LAUNCH,
	GET_STATE,
	SET_CONFIG,
	RESET
}

struct Message {
	1: MessageType type,
	2: i32 id,
}

enum LaunchTubeState {
	ARMED,
	FIRED,
}

struct LaunchTube {
	1: byte position,
	2: optional LaunchTubeState state,
}

struct Launch {
	1: list<LaunchTube> tubes,
}

struct LaunchResponse {
}

struct GetLaunchTubeState {
}

struct GetLaunchTubeStateResponse {
	1: list<LaunchTube> tubes,
}

struct Error {
}

