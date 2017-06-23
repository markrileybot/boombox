package boombox.android.blespp;

/**
 * Created by mriley on 11/18/16.
 */
public enum GattStatus {
	GATT_UNKNOWN(-0x01),

	GATT_SUCCESS(0x00),
	GATT_INVALID_HANDLE(0x01),
	GATT_READ_NOT_PERMIT(0x02),
	GATT_WRITE_NOT_PERMIT(0x03),
	GATT_INVALID_PDU(0x04),
	GATT_INSUF_AUTHENTICATION(0x05),
	GATT_REQ_NOT_SUPPORTED(0x06),
	GATT_INVALID_OFFSET(0x07),
	GATT_INSUF_AUTHORIZATION(0x08),
	GATT_PREPARE_Q_FULL(0x09),
	GATT_NOT_FOUND(0x0a),
	GATT_NOT_LONG(0x0b),
	GATT_INSUF_KEY_SIZE(0x0c),
	GATT_INVALID_ATTR_LEN(0x0d),
	GATT_ERR_UNLIKELY(0x0e),
	GATT_INSUF_ENCRYPTION(0x0f),
	GATT_UNSUPPORT_GRP_TYPE(0x10),
	GATT_INSUF_RESOURCE(0x11),


	GATT_ILLEGAL_PARAMETER(0x87),
	GATT_NO_RESOURCES(0x80),
	GATT_INTERNAL_ERROR(0x81),
	GATT_WRONG_STATE(0x82),
	GATT_DB_FULL(0x83),
	GATT_BUSY(0x84),
	GATT_ERROR(0x85),
	GATT_CMD_STARTED(0x86),
	GATT_PENDING(0x88),
	GATT_AUTH_FAIL(0x89),
	GATT_MORE(0x8a),
	GATT_INVALID_CFG(0x8b),
	GATT_SERVICE_STARTED(0x8c),
	GATT_ENCRYPED_NO_MITM(0x8d),
	GATT_NOT_ENCRYPTED(0x8e),
	GATT_CONGESTED(0x8f),
	GATT_FAILURE(0x101),

	/*(0xE0), ~(0xFC), reserved for future use */
	GATT_CCC_CFG_ERR(0xFD), /* Client Characteristic Configuration Descriptor Improperly Configured */
	GATT_PRC_IN_PROGRESS(0xFE), /* Procedure Already in progress */
	GATT_OUT_OF_RANGE(0xFF); /* Attribute value out of range */

	public static final GattStatus[] VALUES = values();
	private final int code;

	GattStatus(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static GattStatus getStatus(int code) {
		for(GattStatus gs : VALUES) {
			if(gs.code == code) {
				return gs;
			}
		}
		return GATT_UNKNOWN;
	}
}
