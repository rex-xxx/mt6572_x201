/*
 * P2P - IE parser
 * Copyright (c) 2009-2010, Atheros Communications
 *
 * This software may be distributed under the terms of the BSD license.
 * See README for more details.
 */

#include "includes.h"

#include "common.h"
#include "common/ieee802_11_defs.h"
#include "common/ieee802_11_common.h"
#include "wps/wps_i.h"
#include "p2p_i.h"


#if CONFIG_MTK_WFD
static int wfd_parse_attribute(u8 id, const u8 *data, u16 len,
			       struct p2p_message *msg)
{
	const u8 *pos;
	size_t i, nlen;

	if(len > 0){
		switch (id) {
			case WFD_SUBELEMENT_ID_WFD_DEV_INFO:
            {
			    //P_WFD_ATTRI_WFD_DEV_INFO_T prWfdAttriWfdDevInfo = NULL;
			    //prWfdAttriWfdDevInfo  = (P_WFD_ATTRI_WFD_DEV_INFO_T)prWfdAttribute;
                if(len == WFD_ATTRI_MAX_LEN_WFD_DEV_INFO) {
			        //WLAN_GET_FIELD_BE16(&prWfdAttriWfdDevInfo->u2DevInfo,&prWfdInfo->u2DevInfo);
					//WLAN_GET_FIELD_BE16(&prWfdAttriWfdDevInfo->u2ControlPort,&prWfdInfo->u2ControlPort);
					//WLAN_GET_FIELD_BE16(&prWfdAttriWfdDevInfo->u2MaximumTp,&prWfdInfo->u2MaximumTp);
                    //CLEAR_FLAGS(prWfdInfo->u4Flag, WFD_FLAGS_DEVICE_INFO_FROM_SESSOIN_INFO);
                    //DBGLOG(P2P, TRACE, ("WFD_DEV_INFO Dev 0x%x Port 0x%x Tp 0x%x\n",
                    //            prWfdInfo->u2DevInfo,
                    //            prWfdInfo->u2ControlPort,
                    //            prWfdInfo->u2MaximumTp
                    //            ));
                    //      prWfdInfo->ucEnable = TRUE;
                    msg->wfd_device_info = data;
                }
			}
                break;
            case WFD_SUBELEMENT_ID_ASSOCIATED_BSSID:
            {
                //P_WFD_ATTRI_ASSOCIATED_BSSID_T prWfdAttriAssocBssid = NULL;
                //prWfdAttriAssocBssid = (P_WFD_ATTRI_ASSOCIATED_BSSID_T)prWfdAttribute;
                if(len == WFD_ATTRI_MAX_LEN_ASSOCIATED_BSSID) {
                    msg->wfd_associated_bssid = data;
                    //COPY_MAC_ADDR(prWfdInfo->aucAssociatedBssid, prWfdAttriAssocBssid->aucAssociatedBssid);
                    //DBGLOG(P2P, TRACE, ("ASSOCIATED " MACSTR "\n", MAC2STR(prWfdInfo->aucAssociatedBssid)));
                }
            }
                break;
            case WFD_SUBELEMENT_ID_COUPLED_SINK_INFO:
            {
                //P_WFD_ATTRI_COUPLED_SINK_STATUS_T prWfdAttriCoupldSinkStatus = NULL;
                //prWfdAttriCoupldSinkStatus = (P_WFD_ATTRI_COUPLED_SINK_STATUS_T)prWfdAttribute;
                if(len == WFD_ATTRI_MAX_LEN_COUPLED_SINK_INFO) {
					//prWfdInfo->ucCoupledSinkStatus = prWfdAttriCoupldSinkStatus->ucCoupledSinkStatus;
                    //kalMemCopy(prWfdInfo->aucCoupledSinkAddress, prWfdAttriCoupldSinkStatus->aucCoupledSinkAddress, MAC_ADDR_LEN);
                    msg->wfd_coupled_sink_info = data;
                    //DBGLOG(P2P, TRACE, ("SINK INFO %x " MACSTR "\n", prWfdInfo->ucCoupledSinkStatus,
                    //            MAC2STR(prWfdInfo->aucCoupledSinkAddress)));
                }
            }
                break;
            case WFD_SUBELEMENT_ID_WFD_SESSION_INFO:
                if(len >= WFD_ATTRI_MAX_LEN_WFD_SESSION_INFO){
                    msg->wfd_session_info = data;
                    msg->wfd_session_info_len = len;
                }
                break;
			case WFD_SUBELEMENT_ID_WFD_EXT_CAPA_BITMAP_INFO:
                if(len == WFD_ATTRI_MAX_LEN_EXT_CAPA_BITMAP_INFO){
                    msg->wfd_ext_capa_bitmap_info = data;
                }
				break;
                default:
                    break;
        } /* switch (id) */
    }
	return 0;
}


/**
 * p2p_parse_wfd_ie - Parse WFD IE
 * @buf: Concatenated WFD IE(s) payload
 * @msg: Buffer for returning parsed attributes
 * Returns: 0 on success, -1 on failure
 *
 * Note: Caller is responsible for clearing the msg data structure before
 * calling this function.
 */
int p2p_parse_wfd_ie(const struct wpabuf *buf, struct p2p_message *msg)
{
	const u8 *pos = wpabuf_head_u8(buf);
	const u8 *end = pos + wpabuf_len(buf);

	wpa_printf(MSG_DEBUG, "P2P: Parsing WFD IE : buflen=%d", wpabuf_len(buf));

	while (pos < end) {
		u16 attr_len;
		if (pos + 2 >= end) {
			wpa_printf(MSG_DEBUG, "P2P: Invalid WFD attribute");
			return -1;
		}
		attr_len = WPA_GET_BE16(pos + 1);
		wpa_printf(MSG_DEBUG, "P2P: WFD Attribute %d length %u",
			   pos[0], attr_len);
		if (pos + 3 + attr_len > end) {
			wpa_printf(MSG_DEBUG, "P2P: WFD Attribute underflow "
				   "(len=%u left=%d)",
				   attr_len, (int) (end - pos - 3));
			wpa_hexdump(MSG_MSGDUMP, "P2P: WFD Data", pos, end - pos);
			return -1;
		}
		if (wfd_parse_attribute(pos[0], pos + 3, attr_len, msg))
			return -1;
		pos += 3 + attr_len;
	}

	return 0;
}
#endif /* CONFIG_MTK_WFD */


static int p2p_parse_attribute(u8 id, const u8 *data, u16 len,
			       struct p2p_message *msg)
{
	const u8 *pos;
	size_t i, nlen;
	char devtype[WPS_DEV_TYPE_BUFSIZE];

	switch (id) {
	case P2P_ATTR_CAPABILITY:
		if (len < 2) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Capability "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->capability = data;
		wpa_printf(MSG_DEBUG, "P2P: * Device Capability %02x "
			   "Group Capability %02x",
			   data[0], data[1]);
		break;
	case P2P_ATTR_DEVICE_ID:
		if (len < ETH_ALEN) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Device ID "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->device_id = data;
		wpa_printf(MSG_DEBUG, "P2P: * Device ID " MACSTR,
			   MAC2STR(msg->device_id));
		break;
	case P2P_ATTR_GROUP_OWNER_INTENT:
		if (len < 1) {
			wpa_printf(MSG_DEBUG, "P2P: Too short GO Intent "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->go_intent = data;
		wpa_printf(MSG_DEBUG, "P2P: * GO Intent: Intent %u "
			   "Tie breaker %u", data[0] >> 1, data[0] & 0x01);
		break;
	case P2P_ATTR_STATUS:
		if (len < 1) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Status "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->status = data;
		wpa_printf(MSG_DEBUG, "P2P: * Status: %d", data[0]);
		break;
	case P2P_ATTR_LISTEN_CHANNEL:
		if (len == 0) {
			wpa_printf(MSG_DEBUG, "P2P: * Listen Channel: Ignore "
				   "null channel");
			break;
		}
		if (len < 5) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Listen Channel "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->listen_channel = data;
		wpa_printf(MSG_DEBUG, "P2P: * Listen Channel: "
			   "Country %c%c(0x%02x) Regulatory "
			   "Class %d Channel Number %d", data[0], data[1],
			   data[2], data[3], data[4]);
		break;
	case P2P_ATTR_OPERATING_CHANNEL:
		if (len == 0) {
			wpa_printf(MSG_DEBUG, "P2P: * Operating Channel: "
				   "Ignore null channel");
			break;
		}
		if (len < 5) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Operating "
				   "Channel attribute (length %d)", len);
			return -1;
		}
		msg->operating_channel = data;
		wpa_printf(MSG_DEBUG, "P2P: * Operating Channel: "
			   "Country %c%c(0x%02x) Regulatory "
			   "Class %d Channel Number %d", data[0], data[1],
			   data[2], data[3], data[4]);
		break;
	case P2P_ATTR_CHANNEL_LIST:
		if (len < 3) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Channel List "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->channel_list = data;
		msg->channel_list_len = len;
		wpa_printf(MSG_DEBUG, "P2P: * Channel List: Country String "
			   "'%c%c(0x%02x)'", data[0], data[1], data[2]);
		wpa_hexdump(MSG_MSGDUMP, "P2P: Channel List",
			    msg->channel_list, msg->channel_list_len);
		break;
	case P2P_ATTR_GROUP_INFO:
		msg->group_info = data;
		msg->group_info_len = len;
		wpa_printf(MSG_DEBUG, "P2P: * Group Info");
		break;
	case P2P_ATTR_DEVICE_INFO:
		if (len < ETH_ALEN + 2 + 8 + 1) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Device Info "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->p2p_device_info = data;
		msg->p2p_device_info_len = len;
		pos = data;
		msg->p2p_device_addr = pos;
		pos += ETH_ALEN;
		msg->config_methods = WPA_GET_BE16(pos);
		pos += 2;
		msg->pri_dev_type = pos;
		pos += 8;
		msg->num_sec_dev_types = *pos++;
		if (msg->num_sec_dev_types * 8 > data + len - pos) {
			wpa_printf(MSG_DEBUG, "P2P: Device Info underflow");
			return -1;
		}
		pos += msg->num_sec_dev_types * 8;
		if (data + len - pos < 4) {
			wpa_printf(MSG_DEBUG, "P2P: Invalid Device Name "
				   "length %d", (int) (data + len - pos));
			return -1;
		}
		if (WPA_GET_BE16(pos) != ATTR_DEV_NAME) {
			wpa_hexdump(MSG_DEBUG, "P2P: Unexpected Device Name "
				    "header", pos, 4);
			return -1;
		}
		pos += 2;
		nlen = WPA_GET_BE16(pos);
		pos += 2;
		if (data + len - pos < (int) nlen || nlen > 32) {
			wpa_printf(MSG_DEBUG, "P2P: Invalid Device Name "
				   "length %d (buf len %d)", (int) nlen,
				   (int) (data + len - pos));
			return -1;
		}
		os_memcpy(msg->device_name, pos, nlen);
		msg->device_name[nlen] = '\0';
		for (i = 0; i < nlen; i++) {
			if (msg->device_name[i] == '\0')
				break;
			if (msg->device_name[i] > 0 &&
			    msg->device_name[i] < 32)
				msg->device_name[i] = '_';
		}
		wpa_printf(MSG_DEBUG, "P2P: * Device Info: addr " MACSTR
			   " primary device type %s device name '%s' "
			   "config methods 0x%x",
			   MAC2STR(msg->p2p_device_addr),
			   wps_dev_type_bin2str(msg->pri_dev_type, devtype,
						sizeof(devtype)),
			   msg->device_name, msg->config_methods);
		break;
	case P2P_ATTR_CONFIGURATION_TIMEOUT:
		if (len < 2) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Configuration "
				   "Timeout attribute (length %d)", len);
			return -1;
		}
		msg->config_timeout = data;
		wpa_printf(MSG_DEBUG, "P2P: * Configuration Timeout");
		break;
	case P2P_ATTR_INTENDED_INTERFACE_ADDR:
		if (len < ETH_ALEN) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Intended P2P "
				   "Interface Address attribute (length %d)",
				   len);
			return -1;
		}
		msg->intended_addr = data;
		wpa_printf(MSG_DEBUG, "P2P: * Intended P2P Interface Address: "
			   MACSTR, MAC2STR(msg->intended_addr));
		break;
	case P2P_ATTR_GROUP_BSSID:
		if (len < ETH_ALEN) {
			wpa_printf(MSG_DEBUG, "P2P: Too short P2P Group BSSID "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->group_bssid = data;
		wpa_printf(MSG_DEBUG, "P2P: * P2P Group BSSID: " MACSTR,
			   MAC2STR(msg->group_bssid));
		break;
	case P2P_ATTR_GROUP_ID:
		if (len < ETH_ALEN || len > ETH_ALEN + 32) {
			wpa_printf(MSG_DEBUG, "P2P: Invalid P2P Group ID "
				   "attribute length %d", len);
			return -1;
		}
		msg->group_id = data;
		msg->group_id_len = len;
		wpa_printf(MSG_DEBUG, "P2P: * P2P Group ID: Device Address "
			   MACSTR, MAC2STR(msg->group_id));
		wpa_hexdump_ascii(MSG_DEBUG, "P2P: * P2P Group ID: SSID",
				  msg->group_id + ETH_ALEN,
				  msg->group_id_len - ETH_ALEN);
		break;
	case P2P_ATTR_INVITATION_FLAGS:
		if (len < 1) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Invitation "
				   "Flag attribute (length %d)", len);
			return -1;
		}
		msg->invitation_flags = data;
		wpa_printf(MSG_DEBUG, "P2P: * Invitation Flags: bitmap 0x%x",
			   data[0]);
		break;
	case P2P_ATTR_MANAGEABILITY:
		if (len < 1) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Manageability "
				   "attribute (length %d)", len);
			return -1;
		}
		msg->manageability = data;
		wpa_printf(MSG_DEBUG, "P2P: * Manageability: bitmap 0x%x",
			   data[0]);
		break;
	case P2P_ATTR_NOTICE_OF_ABSENCE:
		if (len < 2) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Notice of "
				   "Absence attribute (length %d)", len);
			return -1;
		}
		msg->noa = data;
		msg->noa_len = len;
		wpa_printf(MSG_DEBUG, "P2P: * Notice of Absence");
		break;
	case P2P_ATTR_EXT_LISTEN_TIMING:
		if (len < 4) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Extended Listen "
				   "Timing attribute (length %d)", len);
			return -1;
		}
		msg->ext_listen_timing = data;
		wpa_printf(MSG_DEBUG, "P2P: * Extended Listen Timing "
			   "(period %u msec  interval %u msec)",
			   WPA_GET_LE16(msg->ext_listen_timing),
			   WPA_GET_LE16(msg->ext_listen_timing + 2));
		break;
	case P2P_ATTR_MINOR_REASON_CODE:
		if (len < 1) {
			wpa_printf(MSG_DEBUG, "P2P: Too short Minor Reason "
				   "Code attribute (length %d)", len);
			return -1;
		}
		msg->minor_reason_code = data;
		wpa_printf(MSG_DEBUG, "P2P: * Minor Reason Code: %u",
			   *msg->minor_reason_code);
		break;
	default:
		wpa_printf(MSG_DEBUG, "P2P: Skipped unknown attribute %d "
			   "(length %d)", id, len);
		break;
	}

	return 0;
}


/**
 * p2p_parse_p2p_ie - Parse P2P IE
 * @buf: Concatenated P2P IE(s) payload
 * @msg: Buffer for returning parsed attributes
 * Returns: 0 on success, -1 on failure
 *
 * Note: Caller is responsible for clearing the msg data structure before
 * calling this function.
 */
int p2p_parse_p2p_ie(const struct wpabuf *buf, struct p2p_message *msg)
{
	const u8 *pos = wpabuf_head_u8(buf);
	const u8 *end = pos + wpabuf_len(buf);

	wpa_printf(MSG_DEBUG, "P2P: Parsing P2P IE");

	while (pos < end) {
		u16 attr_len;
		if (pos + 2 >= end) {
			wpa_printf(MSG_DEBUG, "P2P: Invalid P2P attribute");
			return -1;
		}
		attr_len = WPA_GET_LE16(pos + 1);
		wpa_printf(MSG_DEBUG, "P2P: Attribute %d length %u",
			   pos[0], attr_len);
		if (pos + 3 + attr_len > end) {
			wpa_printf(MSG_DEBUG, "P2P: Attribute underflow "
				   "(len=%u left=%d)",
				   attr_len, (int) (end - pos - 3));
			wpa_hexdump(MSG_MSGDUMP, "P2P: Data", pos, end - pos);
			return -1;
		}
		if (p2p_parse_attribute(pos[0], pos + 3, attr_len, msg))
			return -1;
		pos += 3 + attr_len;
	}

	return 0;
}


static int p2p_parse_wps_ie(const struct wpabuf *buf, struct p2p_message *msg)
{
	struct wps_parse_attr attr;
	int i;

	wpa_printf(MSG_DEBUG, "P2P: Parsing WPS IE");
	if (wps_parse_msg(buf, &attr))
		return -1;
	if (attr.dev_name && attr.dev_name_len < sizeof(msg->device_name) &&
	    !msg->device_name[0])
		os_memcpy(msg->device_name, attr.dev_name, attr.dev_name_len);
	if (attr.config_methods) {
		msg->wps_config_methods =
			WPA_GET_BE16(attr.config_methods);
		wpa_printf(MSG_DEBUG, "P2P: Config Methods (WPS): 0x%x",
			   msg->wps_config_methods);
	}
	if (attr.dev_password_id) {
		msg->dev_password_id = WPA_GET_BE16(attr.dev_password_id);
		wpa_printf(MSG_DEBUG, "P2P: Device Password ID: %d",
			   msg->dev_password_id);
	}
	if (attr.primary_dev_type) {
		char devtype[WPS_DEV_TYPE_BUFSIZE];
		msg->wps_pri_dev_type = attr.primary_dev_type;
		wpa_printf(MSG_DEBUG, "P2P: Primary Device Type (WPS): %s",
			   wps_dev_type_bin2str(msg->wps_pri_dev_type, devtype,
						sizeof(devtype)));
	}
	if (attr.sec_dev_type_list) {
		msg->wps_sec_dev_type_list = attr.sec_dev_type_list;
		msg->wps_sec_dev_type_list_len = attr.sec_dev_type_list_len;
	}

	for (i = 0; i < P2P_MAX_WPS_VENDOR_EXT; i++) {
		msg->wps_vendor_ext[i] = attr.vendor_ext[i];
		msg->wps_vendor_ext_len[i] = attr.vendor_ext_len[i];
	}

	msg->manufacturer = attr.manufacturer;
	msg->manufacturer_len = attr.manufacturer_len;
	msg->model_name = attr.model_name;
	msg->model_name_len = attr.model_name_len;
	msg->model_number = attr.model_number;
	msg->model_number_len = attr.model_number_len;
	msg->serial_number = attr.serial_number;
	msg->serial_number_len = attr.serial_number_len;

	return 0;
}


/**
 * p2p_parse_ies - Parse P2P message IEs (both WPS and P2P IE)
 * @data: IEs from the message
 * @len: Length of data buffer in octets
 * @msg: Buffer for returning parsed attributes
 * Returns: 0 on success, -1 on failure
 *
 * Note: Caller is responsible for clearing the msg data structure before
 * calling this function.
 *
 * Note: Caller must free temporary memory allocations by calling
 * p2p_parse_free() when the parsed data is not needed anymore.
 */
int p2p_parse_ies(const u8 *data, size_t len, struct p2p_message *msg)
{
	struct ieee802_11_elems elems;

	ieee802_11_parse_elems(data, len, &elems, 0);
	if (elems.ds_params && elems.ds_params_len >= 1)
		msg->ds_params = elems.ds_params;
	if (elems.ssid)
		msg->ssid = elems.ssid - 2;

	msg->wps_attributes = ieee802_11_vendor_ie_concat(data, len,
							  WPS_DEV_OUI_WFA);
	if (msg->wps_attributes &&
	    p2p_parse_wps_ie(msg->wps_attributes, msg)) {
		p2p_parse_free(msg);
		return -1;
	}

	msg->p2p_attributes = ieee802_11_vendor_ie_concat(data, len,
							  P2P_IE_VENDOR_TYPE);
	if (msg->p2p_attributes &&
	    p2p_parse_p2p_ie(msg->p2p_attributes, msg)) {
		wpa_printf(MSG_DEBUG, "P2P: Failed to parse P2P IE data");
		if (msg->p2p_attributes)
			wpa_hexdump_buf(MSG_MSGDUMP, "P2P: P2P IE data",
					msg->p2p_attributes);
		p2p_parse_free(msg);
		return -1;
	}

#ifdef CONFIG_WIFI_DISPLAY
	if (elems.wfd) {
		msg->wfd_subelems = ieee802_11_vendor_ie_concat(
			data, len, WFD_IE_VENDOR_TYPE);
#ifdef CONFIG_MTK_WFD
	    if(p2p_parse_wfd_ie(msg->wfd_subelems, msg)!=0) {
            wpa_printf(MSG_DEBUG, "P2P: Failed to parse WFD IE data");
            if (msg->wfd_subelems)
                wpa_hexdump_buf(MSG_MSGDUMP, "P2P: WFD IE data",
					msg->wfd_subelems);
        }
#endif
	}
#endif /* CONFIG_WIFI_DISPLAY */

	return 0;
}


/**
 * p2p_parse - Parse a P2P Action frame contents
 * @data: Action frame payload after Category and Code fields
 * @len: Length of data buffer in octets
 * @msg: Buffer for returning parsed attributes
 * Returns: 0 on success, -1 on failure
 *
 * Note: Caller must free temporary memory allocations by calling
 * p2p_parse_free() when the parsed data is not needed anymore.
 */
int p2p_parse(const u8 *data, size_t len, struct p2p_message *msg)
{
	os_memset(msg, 0, sizeof(*msg));
	wpa_printf(MSG_DEBUG, "P2P: Parsing the received message");
	if (len < 1) {
		wpa_printf(MSG_DEBUG, "P2P: No Dialog Token in the message");
		return -1;
	}
	msg->dialog_token = data[0];
	wpa_printf(MSG_DEBUG, "P2P: * Dialog Token: %d", msg->dialog_token);

	return p2p_parse_ies(data + 1, len - 1, msg);
}


/**
 * p2p_parse_free - Free temporary data from P2P parsing
 * @msg: Parsed attributes
 */
void p2p_parse_free(struct p2p_message *msg)
{
	wpabuf_free(msg->p2p_attributes);
	msg->p2p_attributes = NULL;
	wpabuf_free(msg->wps_attributes);
	msg->wps_attributes = NULL;
#ifdef CONFIG_WIFI_DISPLAY
	wpabuf_free(msg->wfd_subelems);
	msg->wfd_subelems = NULL;
#endif /* CONFIG_WIFI_DISPLAY */
}


int p2p_group_info_parse(const u8 *gi, size_t gi_len,
			 struct p2p_group_info *info)
{
	const u8 *g, *gend;

	os_memset(info, 0, sizeof(*info));
	if (gi == NULL)
		return 0;

	g = gi;
	gend = gi + gi_len;
	while (g < gend) {
		struct p2p_client_info *cli;
		const u8 *t, *cend;
		int count;

		cli = &info->client[info->num_clients];
		cend = g + 1 + g[0];
		if (cend > gend)
			return -1; /* invalid data */
		/* g at start of P2P Client Info Descriptor */
		/* t at Device Capability Bitmap */
		t = g + 1 + 2 * ETH_ALEN;
		if (t > cend)
			return -1; /* invalid data */
		cli->p2p_device_addr = g + 1;
		cli->p2p_interface_addr = g + 1 + ETH_ALEN;
		cli->dev_capab = t[0];

		if (t + 1 + 2 + 8 + 1 > cend)
			return -1; /* invalid data */

		cli->config_methods = WPA_GET_BE16(&t[1]);
		cli->pri_dev_type = &t[3];

		t += 1 + 2 + 8;
		/* t at Number of Secondary Device Types */
		cli->num_sec_dev_types = *t++;
		if (t + 8 * cli->num_sec_dev_types > cend)
			return -1; /* invalid data */
		cli->sec_dev_types = t;
		t += 8 * cli->num_sec_dev_types;

		/* t at Device Name in WPS TLV format */
		if (t + 2 + 2 > cend)
			return -1; /* invalid data */
		if (WPA_GET_BE16(t) != ATTR_DEV_NAME)
			return -1; /* invalid Device Name TLV */
		t += 2;
		count = WPA_GET_BE16(t);
		t += 2;
		if (count > cend - t)
			return -1; /* invalid Device Name TLV */
		if (count >= 32)
			count = 32;
		cli->dev_name = (const char *) t;
		cli->dev_name_len = count;

		g = cend;

		info->num_clients++;
		if (info->num_clients == P2P_MAX_GROUP_ENTRIES)
			return -1;
	}

	return 0;
}


static int p2p_group_info_text(const u8 *gi, size_t gi_len, char *buf,
			       char *end)
{
	char *pos = buf;
	int ret;
	struct p2p_group_info info;
	unsigned int i;

	if (p2p_group_info_parse(gi, gi_len, &info) < 0)
		return 0;

	for (i = 0; i < info.num_clients; i++) {
		struct p2p_client_info *cli;
		char name[33];
		char devtype[WPS_DEV_TYPE_BUFSIZE];
		u8 s;
		int count;

		cli = &info.client[i];
		ret = os_snprintf(pos, end - pos, "p2p_group_client: "
				  "dev=" MACSTR " iface=" MACSTR,
				  MAC2STR(cli->p2p_device_addr),
				  MAC2STR(cli->p2p_interface_addr));
		if (ret < 0 || ret >= end - pos)
			return pos - buf;
		pos += ret;

		ret = os_snprintf(pos, end - pos,
				  " dev_capab=0x%x config_methods=0x%x "
				  "dev_type=%s",
				  cli->dev_capab, cli->config_methods,
				  wps_dev_type_bin2str(cli->pri_dev_type,
						       devtype,
						       sizeof(devtype)));
		if (ret < 0 || ret >= end - pos)
			return pos - buf;
		pos += ret;

		for (s = 0; s < cli->num_sec_dev_types; s++) {
			ret = os_snprintf(pos, end - pos, " dev_type=%s",
					  wps_dev_type_bin2str(
						  &cli->sec_dev_types[s * 8],
						  devtype, sizeof(devtype)));
			if (ret < 0 || ret >= end - pos)
				return pos - buf;
			pos += ret;
		}

		os_memcpy(name, cli->dev_name, cli->dev_name_len);
		name[cli->dev_name_len] = '\0';
		count = (int) cli->dev_name_len - 1;
		while (count >= 0) {
			if (name[count] > 0 && name[count] < 32)
				name[count] = '_';
			count--;
		}

		ret = os_snprintf(pos, end - pos, " dev_name='%s'\n", name);
		if (ret < 0 || ret >= end - pos)
			return pos - buf;
		pos += ret;
	}

	return pos - buf;
}


/**
 * p2p_attr_text - Build text format description of P2P IE attributes
 * @data: P2P IE contents
 * @buf: Buffer for returning text
 * @end: Pointer to the end of the buf area
 * Returns: Number of octets written to the buffer or -1 on faikure
 *
 * This function can be used to parse P2P IE contents into text format
 * field=value lines.
 */
int p2p_attr_text(struct wpabuf *data, char *buf, char *end)
{
	struct p2p_message msg;
	char *pos = buf;
	int ret;

	os_memset(&msg, 0, sizeof(msg));
	if (p2p_parse_p2p_ie(data, &msg))
		return -1;

	if (msg.capability) {
		ret = os_snprintf(pos, end - pos,
				  "p2p_dev_capab=0x%x\n"
				  "p2p_group_capab=0x%x\n",
				  msg.capability[0], msg.capability[1]);
		if (ret < 0 || ret >= end - pos)
			return pos - buf;
		pos += ret;
	}

	if (msg.pri_dev_type) {
		char devtype[WPS_DEV_TYPE_BUFSIZE];
		ret = os_snprintf(pos, end - pos,
				  "p2p_primary_device_type=%s\n",
				  wps_dev_type_bin2str(msg.pri_dev_type,
						       devtype,
						       sizeof(devtype)));
		if (ret < 0 || ret >= end - pos)
			return pos - buf;
		pos += ret;
	}

	ret = os_snprintf(pos, end - pos, "p2p_device_name=%s\n",
			  msg.device_name);
	if (ret < 0 || ret >= end - pos)
		return pos - buf;
	pos += ret;

	if (msg.p2p_device_addr) {
		ret = os_snprintf(pos, end - pos, "p2p_device_addr=" MACSTR
				  "\n",
				  MAC2STR(msg.p2p_device_addr));
		if (ret < 0 || ret >= end - pos)
			return pos - buf;
		pos += ret;
	}

	ret = os_snprintf(pos, end - pos, "p2p_config_methods=0x%x\n",
			  msg.config_methods);
	if (ret < 0 || ret >= end - pos)
		return pos - buf;
	pos += ret;

	ret = p2p_group_info_text(msg.group_info, msg.group_info_len,
				  pos, end);
	if (ret < 0)
		return pos - buf;
	pos += ret;

	return pos - buf;
}


int p2p_get_cross_connect_disallowed(const struct wpabuf *p2p_ie)
{
	struct p2p_message msg;

	os_memset(&msg, 0, sizeof(msg));
	if (p2p_parse_p2p_ie(p2p_ie, &msg))
		return 0;

	if (!msg.manageability)
		return 0;

	return !(msg.manageability[0] & P2P_MAN_CROSS_CONNECTION_PERMITTED);
}


u8 p2p_get_group_capab(const struct wpabuf *p2p_ie)
{
	struct p2p_message msg;

	os_memset(&msg, 0, sizeof(msg));
	if (p2p_parse_p2p_ie(p2p_ie, &msg))
		return 0;

	if (!msg.capability)
		return 0;

	return msg.capability[1];
}


const u8 * p2p_get_go_dev_addr(const struct wpabuf *p2p_ie)
{
	struct p2p_message msg;

	os_memset(&msg, 0, sizeof(msg));
	if (p2p_parse_p2p_ie(p2p_ie, &msg))
		return NULL;

	if (msg.p2p_device_addr)
		return msg.p2p_device_addr;
	if (msg.device_id)
		return msg.device_id;

	return NULL;
}

#ifdef CONFIG_MTK_WFD
static int wfd_session_info_text(const u8 *si, size_t si_len, char *buf,
			       char *end)
{
	char *pos = buf;
	int ret;
	struct wfd_session_info info;
	unsigned int i;

	if (wfd_session_info_parse(si, si_len, &info) < 0)
		return 0;

	for (i = 0; i < info.num_clients; i++) {
		struct wfd_client_info *cli;

		cli = &info.client[i];
		ret = os_snprintf(pos, end - pos, "wfd_session_info:"
				  " dev=" MACSTR 
                  " info=0x%x tp=%u"
				  " assoc_mac=" MACSTR
				  " sink_status=0x%x sink_mac=" MACSTR "\n",
				  MAC2STR(cli->wfd_device_addr),
                  cli->wfd_dev_info,
                  cli->wfd_max_throughput,
				  MAC2STR(cli->wfd_associated_bssid),
                  cli->wfd_sink_status,
				  MAC2STR(cli->wfd_sink_mac)
                  );

		if (ret < 0 || ret >= end - pos)
			return pos - buf;
		pos += ret;

	}

	return pos - buf;
}


int wfd_session_info_parse(const u8 *si, size_t si_len,
			 struct wfd_session_info *info)
{
	const u8 *s, *send;
    P_WFD_DEV_INFO_DESC_T pCliDesc;

    //wpa_printf(MSG_DEBUG, "WFD: wfd_session_info_parse %u", si_len);
	if (si == NULL)
		return -1;

	if (si_len == 0)
		return -1;

	os_memset(info, 0, sizeof(*info));

	s = si;
	send = si + si_len;


	while (s < send) {
		struct wfd_client_info *cli;
		const u8 *cend;

		cli = &info->client[info->num_clients];

		cend = s + WFD_SESSION_INFO_DESC_HDR_LEN + s[0];
        
		if (cend > send) {
			return -1; /* invalid data */
        }

        pCliDesc = (P_WFD_DEV_INFO_DESC_T)s;

        if(pCliDesc->ucLength != WFD_ATTRI_MAX_LEN_WFD_SESSION_INFO) {
            wpa_printf(MSG_DEBUG, "WFD: Wrong client descriptor length (%u) in session information", s[0]);
            return -1;
        }

		cli->wfd_device_addr = pCliDesc->aucDeviceAddress;
		//os_memcpy(cli->wfd_device_addr,pCliDesc->aucDeviceAddress,ETH_ALEN);
		cli->wfd_dev_info = WPA_GET_BE16((u8 *)&pCliDesc->u2DevInfo);
		cli->wfd_max_throughput = WPA_GET_BE16( (u8 *)&pCliDesc->u2MaximumTp);
        cli->wfd_associated_bssid = pCliDesc->aucAssociatedBssid;
        //os_memcpy(cli->wfd_associated_bssid,pCliDesc->aucAssociatedBssid,ETH_ALEN);
		cli->wfd_sink_status = pCliDesc->ucCoupledSinkStatus;
        cli->wfd_sink_mac = pCliDesc->aucCoupledSinkAddress;
        //os_memcpy(cli->wfd_sink_mac,pCliDesc->aucCoupledSinkAddress,ETH_ALEN);        

        //wpa_printf(MSG_DEBUG, "WFD: dev " MACSTR " info 0x%x tp %u Assoc " MACSTR  " in session information", 
        //        MAC2STR(cli->wfd_device_addr),
        //        cli->wfd_dev_info,
        //        cli->wfd_max_throughput,
        //        MAC2STR(cli->wfd_associated_bssid));
                
		s = cend;

		info->num_clients++;
		if (info->num_clients == P2P_MAX_GROUP_ENTRIES) {
			return -1;
        }
	}

	return 0;
}


int wfd_attr_text(struct wpabuf *data, char *buf, char *end)
{
	struct p2p_message msg;
	char *pos = buf;
	int ret;

    P_WFD_ATTRI_WFD_DEV_INFO_T prWfdDevInfo;
    P_WFD_ATTRI_ASSOCIATED_BSSID_T prWfdAssociatedBssid;
    P_WFD_ATTRI_COUPLED_SINK_STATUS_T prWfdCoupledSinkInfo;
    P_WFD_ATTRI_EXT_CAPA_BITMAP_T prWfdExtCapaBitmap;

	os_memset(&msg, 0, sizeof(msg));
	if (p2p_parse_wfd_ie(data, &msg))
		return -1;

    if(msg.wfd_device_info) {
		ret = os_snprintf(pos, end - pos,
				  "wfd_en=%u\n",1);
    }
    else {
		ret = os_snprintf(pos, end - pos,
				  "wfd_en=%u\n",0);
    }

    if (ret < 0 || ret >= end - pos)
        goto _FINISH;
	pos += ret;

    if(!msg.wfd_device_info) {
        goto _FINISH;
    }


    if(msg.wfd_device_info) {
	    prWfdDevInfo = (P_WFD_ATTRI_WFD_DEV_INFO_T) msg.wfd_device_info;
	   
		ret = os_snprintf(pos, end - pos,
				  "wfd_info=0x%x\n"
				  "wfd_ctrl_port=%u\n"
				  "wfd_tp=%u\n",
            WPA_GET_BE16((u8 *)(&prWfdDevInfo->u2DevInfo)),
            WPA_GET_BE16((u8*)(&prWfdDevInfo->u2ControlPort)),
            WPA_GET_BE16((u8*)(&prWfdDevInfo->u2MaximumTp)));
		if (ret < 0 || ret >= end - pos)
			goto _FINISH;
		pos += ret;

    }


    if(msg.wfd_associated_bssid) {
		prWfdAssociatedBssid  = (P_WFD_ATTRI_ASSOCIATED_BSSID_T) msg.wfd_associated_bssid;

		ret = os_snprintf(pos, end - pos, "wfd_assoc_mac=" MACSTR
				  "\n",
				  MAC2STR(prWfdAssociatedBssid->aucAssociatedBssid));

		if (ret < 0 || ret >= end - pos)
			goto _FINISH;
		pos += ret;


	}

    if(msg.wfd_coupled_sink_info) {
		prWfdCoupledSinkInfo  = (P_WFD_ATTRI_COUPLED_SINK_STATUS_T) msg.wfd_coupled_sink_info;

		ret = os_snprintf(pos, end - pos,
                "wfd_sink_status=0x%x\n"
                "wfd_sink_mac=" MACSTR
				  "\n",
                  prWfdCoupledSinkInfo->ucCoupledSinkStatus,
				  MAC2STR( prWfdCoupledSinkInfo->aucCoupledSinkAddress));
		if (ret < 0 || ret >= end - pos)
			goto _FINISH;
		pos += ret;
    }


    if(msg.wfd_ext_capa_bitmap_info) {
		prWfdExtCapaBitmap = (P_WFD_ATTRI_EXT_CAPA_BITMAP_T) msg.wfd_ext_capa_bitmap_info;

		ret = os_snprintf(pos, end - pos,
                "wfd_extend_capa=0x%x\n",
                  WPA_GET_BE16((u8*)(&prWfdExtCapaBitmap->u2WfdExtCapBitmap)));
		if (ret < 0 || ret >= end - pos)
			goto _FINISH;
		pos += ret;
	}

    if((msg.wfd_session_info_len>0) && msg.wfd_session_info) {

        ret = wfd_session_info_text(msg.wfd_session_info, msg.wfd_session_info_len,
                      pos, end);
        if (ret < 0)
            goto _FINISH;
        pos += ret;
    }

_FINISH:
	return pos - buf;
}

int wfd_attr_text_append(struct wpabuf *data, char *buf, char *end, unsigned int flags)
{
	struct p2p_message msg;
	char *pos = buf;
	int ret;

    P_WFD_ATTRI_WFD_DEV_INFO_T prWfdDevInfo;
    P_WFD_ATTRI_ASSOCIATED_BSSID_T prWfdAssociatedBssid;
    P_WFD_ATTRI_COUPLED_SINK_STATUS_T prWfdCoupledSinkInfo;
    P_WFD_ATTRI_EXT_CAPA_BITMAP_T prWfdExtCapaBitmap;

	os_memset(&msg, 0, sizeof(msg));
	if (p2p_parse_wfd_ie(data, &msg))
		return -1;

    if(msg.wfd_device_info) {
		ret = os_snprintf(pos, end - pos,
				  "wfd_en=%u ",1);
    }
    else {
		ret = os_snprintf(pos, end - pos,
				  "wfd_en=%u ",0);
    }

    if (ret < 0 || ret >= end - pos)
        goto _FINISH;
	pos += ret;

    if(!msg.wfd_device_info) {
        goto _FINISH;
    }


    if(msg.wfd_device_info) {
	    prWfdDevInfo = (P_WFD_ATTRI_WFD_DEV_INFO_T) msg.wfd_device_info;
	   
		ret = os_snprintf(pos, end - pos,
				  "wfd_info=0x%x "
				  "wfd_ctrl_port=%u "
				  "wfd_tp=%u ",
            WPA_GET_BE16((u8 *)(&prWfdDevInfo->u2DevInfo)),
            WPA_GET_BE16((u8*)(&prWfdDevInfo->u2ControlPort)),
            WPA_GET_BE16((u8*)(&prWfdDevInfo->u2MaximumTp)));
		if (ret < 0 || ret >= end - pos)
			goto _FINISH;
		pos += ret;

    }


    if(msg.wfd_associated_bssid) {
		prWfdAssociatedBssid  = (P_WFD_ATTRI_ASSOCIATED_BSSID_T) msg.wfd_associated_bssid;

		ret = os_snprintf(pos, end - pos, "wfd_assoc_mac=" MACSTR
				  " ",
				  MAC2STR(prWfdAssociatedBssid->aucAssociatedBssid));

		if (ret < 0 || ret >= end - pos)
			goto _FINISH;
		pos += ret;


	}

    if(msg.wfd_coupled_sink_info) {
		prWfdCoupledSinkInfo  = (P_WFD_ATTRI_COUPLED_SINK_STATUS_T) msg.wfd_coupled_sink_info;

		ret = os_snprintf(pos, end - pos,
                "wfd_sink_status=0x%x "
                "wfd_sink_mac=" MACSTR
				  " ",
                  prWfdCoupledSinkInfo->ucCoupledSinkStatus,
				  MAC2STR( prWfdCoupledSinkInfo->aucCoupledSinkAddress));
		if (ret < 0 || ret >= end - pos)
			goto _FINISH;
		pos += ret;
    }


    if(msg.wfd_ext_capa_bitmap_info) {
		prWfdExtCapaBitmap = (P_WFD_ATTRI_EXT_CAPA_BITMAP_T) msg.wfd_ext_capa_bitmap_info;

		ret = os_snprintf(pos, end - pos,
                "wfd_extend_capa=0x%x ",
                  WPA_GET_BE16((u8*)(&prWfdExtCapaBitmap->u2WfdExtCapBitmap)));
		if (ret < 0 || ret >= end - pos)
			goto _FINISH;
		pos += ret;
	}

    if((msg.wfd_session_info_len>0) && msg.wfd_session_info) {

        ret = wfd_session_info_text(msg.wfd_session_info, msg.wfd_session_info_len,
                      pos, end);
        if (ret < 0)
            goto _FINISH;
        pos += ret;
    }

_FINISH:
	return pos - buf;
}


#endif /* CONFIG_MTK_WFD */


