/*
 * wpa_supplicant - Wi-Fi Display
 * Copyright (c) 2011, Atheros Communications, Inc.
 * Copyright (c) 2011-2012, Qualcomm Atheros, Inc.
 *
 * This software may be distributed under the terms of the BSD license.
 * See README for more details.
 */

#include "includes.h"

#include "common.h"
#include "p2p/p2p.h"
#include "common/ieee802_11_defs.h"
#include "wpa_supplicant_i.h"
#include "wifi_display.h"


int wifi_display_init(struct wpa_global *global)
{
	global->wifi_display = 1;
#ifdef CONFIG_MTK_WFD
    os_memset(&global->wfd_data, 0, sizeof(struct wfd_data_s));
    os_memset(&global->wfd_data_cmd, 0, sizeof(struct wfd_data_s));
    /* Setting the default value */
    wifi_display_enable(global, 1);
#endif
	return 0;
}


void wifi_display_deinit(struct wpa_global *global)
{
	int i;
	for (i = 0; i < MAX_WFD_SUBELEMS; i++) {
		wpabuf_free(global->wfd_subelem[i]);
		global->wfd_subelem[i] = NULL;
	}
 
}


static int wifi_display_update_wfd_ie(struct wpa_global *global)
{
	struct wpabuf *ie, *buf;
	size_t len, plen;

	wpa_printf(MSG_DEBUG, "WFD: Update WFD IE");

	if (!global->wifi_display) {
		wpa_printf(MSG_DEBUG, "WFD: Wi-Fi Display disabled - do not "
			   "include WFD IE");
		p2p_set_wfd_ie_beacon(global->p2p, NULL);
		p2p_set_wfd_ie_probe_req(global->p2p, NULL);
		p2p_set_wfd_ie_probe_resp(global->p2p, NULL);
		p2p_set_wfd_ie_assoc_req(global->p2p, NULL);
		p2p_set_wfd_ie_invitation(global->p2p, NULL);
		p2p_set_wfd_ie_prov_disc_req(global->p2p, NULL);
		p2p_set_wfd_ie_prov_disc_resp(global->p2p, NULL);
		p2p_set_wfd_ie_go_neg(global->p2p, NULL);
		p2p_set_wfd_dev_info(global->p2p, NULL);
		p2p_set_wfd_assoc_bssid(global->p2p, NULL);
		p2p_set_wfd_coupled_sink_info(global->p2p, NULL);

#ifdef CONFIG_MTK_WFD
        if(global->wfd_data_cmd.WfdEnable) {
            wpa_printf(MSG_DEBUG, "WFD: Warning!!! WfdEnable is TRUE" );
        }
        if(global->p2p_init_wpa_s) {
            wpas_wfd_data_process(global->p2p_init_wpa_s, &global->wfd_data_cmd);
            wpas_wfd_data_update(global->p2p_init_wpa_s, &global->wfd_data);
        }
#endif

		return 0;
	}

	p2p_set_wfd_dev_info(global->p2p,
			     global->wfd_subelem[WFD_SUBELEM_DEVICE_INFO]);
	p2p_set_wfd_assoc_bssid(
		global->p2p,
		global->wfd_subelem[WFD_SUBELEM_ASSOCIATED_BSSID]);
	p2p_set_wfd_coupled_sink_info(
		global->p2p, global->wfd_subelem[WFD_SUBELEM_COUPLED_SINK]);

	/*
	 * WFD IE is included in number of management frames. Two different
	 * sets of subelements are included depending on the frame:
	 *
	 * Beacon, (Re)Association Request, GO Negotiation Req/Resp/Conf,
	 * Provision Discovery Req:
	 * WFD Device Info
	 * [Associated BSSID]
	 * [Coupled Sink Info]
	 *
	 * Probe Request:
	 * WFD Device Info
	 * [Associated BSSID]
	 * [Coupled Sink Info]
	 * [WFD Extended Capability]
	 *
	 * Probe Response:
	 * WFD Device Info
	 * [Associated BSSID]
	 * [Coupled Sink Info]
	 * [WFD Extended Capability]
	 * [WFD Session Info]
	 *
	 * (Re)Association Response, P2P Invitation Req/Resp,
	 * Provision Discovery Resp:
	 * WFD Device Info
	 * [Associated BSSID]
	 * [Coupled Sink Info]
	 * [WFD Session Info]
	 */
	len = 0;
	if (global->wfd_subelem[WFD_SUBELEM_DEVICE_INFO])
		len += wpabuf_len(global->wfd_subelem[
					  WFD_SUBELEM_DEVICE_INFO]);
	if (global->wfd_subelem[WFD_SUBELEM_ASSOCIATED_BSSID])
		len += wpabuf_len(global->wfd_subelem[
					  WFD_SUBELEM_ASSOCIATED_BSSID]);
	if (global->wfd_subelem[WFD_SUBELEM_COUPLED_SINK])
		len += wpabuf_len(global->wfd_subelem[
					  WFD_SUBELEM_COUPLED_SINK]);
	if (global->wfd_subelem[WFD_SUBELEM_SESSION_INFO])
		len += wpabuf_len(global->wfd_subelem[
					  WFD_SUBELEM_SESSION_INFO]);
	if (global->wfd_subelem[WFD_SUBELEM_EXT_CAPAB])
		len += wpabuf_len(global->wfd_subelem[WFD_SUBELEM_EXT_CAPAB]);
	buf = wpabuf_alloc(len);
	if (buf == NULL)
		return -1;

	if (global->wfd_subelem[WFD_SUBELEM_DEVICE_INFO])
		wpabuf_put_buf(buf,
			       global->wfd_subelem[WFD_SUBELEM_DEVICE_INFO]);
	if (global->wfd_subelem[WFD_SUBELEM_ASSOCIATED_BSSID])
		wpabuf_put_buf(buf, global->wfd_subelem[
				       WFD_SUBELEM_ASSOCIATED_BSSID]);
	if (global->wfd_subelem[WFD_SUBELEM_COUPLED_SINK])
		wpabuf_put_buf(buf,
			       global->wfd_subelem[WFD_SUBELEM_COUPLED_SINK]);

	ie = wifi_display_encaps(buf);
	wpa_hexdump_buf(MSG_DEBUG, "WFD: WFD IE for Beacon", ie);
	p2p_set_wfd_ie_beacon(global->p2p, ie);

	ie = wifi_display_encaps(buf);
	wpa_hexdump_buf(MSG_DEBUG, "WFD: WFD IE for (Re)Association Request",
			ie);
	p2p_set_wfd_ie_assoc_req(global->p2p, ie);

	ie = wifi_display_encaps(buf);
	wpa_hexdump_buf(MSG_DEBUG, "WFD: WFD IE for GO Negotiation", ie);
	p2p_set_wfd_ie_go_neg(global->p2p, ie);

	ie = wifi_display_encaps(buf);
	wpa_hexdump_buf(MSG_DEBUG, "WFD: WFD IE for Provision Discovery "
			"Request", ie);
	p2p_set_wfd_ie_prov_disc_req(global->p2p, ie);

	plen = buf->used;
	if (global->wfd_subelem[WFD_SUBELEM_EXT_CAPAB])
		wpabuf_put_buf(buf,
			       global->wfd_subelem[WFD_SUBELEM_EXT_CAPAB]);

	ie = wifi_display_encaps(buf);
	wpa_hexdump_buf(MSG_DEBUG, "WFD: WFD IE for Probe Request", ie);
	p2p_set_wfd_ie_probe_req(global->p2p, ie);

	if (global->wfd_subelem[WFD_SUBELEM_SESSION_INFO])
		wpabuf_put_buf(buf,
			       global->wfd_subelem[WFD_SUBELEM_SESSION_INFO]);
	ie = wifi_display_encaps(buf);
	wpa_hexdump_buf(MSG_DEBUG, "WFD: WFD IE for Probe Response", ie);
	p2p_set_wfd_ie_probe_resp(global->p2p, ie);

	/* Remove WFD Extended Capability from buffer */
	buf->used = plen;
	if (global->wfd_subelem[WFD_SUBELEM_SESSION_INFO])
		wpabuf_put_buf(buf,
			       global->wfd_subelem[WFD_SUBELEM_SESSION_INFO]);

	ie = wifi_display_encaps(buf);
	wpa_hexdump_buf(MSG_DEBUG, "WFD: WFD IE for P2P Invitation", ie);
	p2p_set_wfd_ie_invitation(global->p2p, ie);

	ie = wifi_display_encaps(buf);
	wpa_hexdump_buf(MSG_DEBUG, "WFD: WFD IE for Provision Discovery "
			"Response", ie);
	p2p_set_wfd_ie_prov_disc_resp(global->p2p, ie);

#ifdef CONFIG_MTK_WFD
    {
       const u8 *buf;
       P_WFD_ATTRI_HDR_T prWfdHdr;
       struct wfd_data_s *p_wfd_data_cmd;
       p_wfd_data_cmd = &global->wfd_data_cmd;

       p_wfd_data_cmd->WfdDevInfo = 0;
       p_wfd_data_cmd->WfdControlPort = 0;
       p_wfd_data_cmd->WfdMaximumTp = 0;
       p_wfd_data_cmd->WfdFlag &= ~WFD_FLAG_DEV_INFO_VALID;


        if (global->wfd_subelem[WFD_SUBELEM_DEVICE_INFO]) {
           P_WFD_ATTRI_WFD_DEV_INFO_T prWfdDevInfo = NULL;
           buf = wpabuf_head_u8(global->wfd_subelem[WFD_SUBELEM_DEVICE_INFO]);
           prWfdHdr =  (P_WFD_ATTRI_HDR_T) buf;
           prWfdDevInfo = (P_WFD_ATTRI_WFD_DEV_INFO_T) (buf+WFD_ATTRI_HDR_LEN);

           wpa_printf(MSG_DEBUG,
                      "wfd_id=%u len=%u "
                      "wfd_info=0x%x "
                      "wfd_ctrl_port=%u "
                      "wfd_tp=%u\n",
                prWfdHdr->ucId,
                WPA_GET_BE16((u8 *)(&prWfdHdr->u2Length)),
                WPA_GET_BE16((u8 *)(&prWfdDevInfo->u2DevInfo)),
                WPA_GET_BE16((u8*)(&prWfdDevInfo->u2ControlPort)),
                WPA_GET_BE16((u8*)(&prWfdDevInfo->u2MaximumTp)));

           p_wfd_data_cmd->WfdDevInfo =  WPA_GET_BE16((u8 *)(&prWfdDevInfo->u2DevInfo));
           p_wfd_data_cmd->WfdControlPort = WPA_GET_BE16((u8*)(&prWfdDevInfo->u2ControlPort));
           p_wfd_data_cmd->WfdMaximumTp = WPA_GET_BE16((u8*)(&prWfdDevInfo->u2MaximumTp));
           p_wfd_data_cmd->WfdFlag |= WFD_FLAG_DEV_INFO_VALID;


       }

        if(global->p2p_init_wpa_s) {
            wpas_wfd_data_process(global->p2p_init_wpa_s, &global->wfd_data_cmd);
            wpas_wfd_data_update(global->p2p_init_wpa_s, &global->wfd_data);
        }
    }

#endif /* CONFIG_MTK_WFD */

	wpabuf_free(buf);

	return 0;
}


void wifi_display_enable(struct wpa_global *global, int enabled)
{
	wpa_printf(MSG_DEBUG, "WFD: Wi-Fi Display %s",
		   enabled ? "enabled" : "disabled");
	global->wifi_display = enabled;
#ifdef CONFIG_MTK_WFD
    if(enabled) {
        global->wfd_data_cmd.WfdEnable= 1;
    }
    else {
        global->wfd_data_cmd.WfdEnable= 0;
        global->wfd_data_cmd.WfdState = 0;
    }
#endif
	wifi_display_update_wfd_ie(global);
}


int wifi_display_subelem_set(struct wpa_global *global, char *cmd)
{
	char *pos;
	int subelem;
	size_t len;
	struct wpabuf *e;
#ifdef CONFIG_MTK_WFD
    if(cmd) {
		wpa_printf(MSG_DEBUG, "WFD: subelem_set %s\n",cmd);
    }
#endif
	pos = os_strchr(cmd, ' ');
	if (pos == NULL)
		return -1;
	*pos++ = '\0';
	subelem = atoi(cmd);
	if (subelem < 0 || subelem >= MAX_WFD_SUBELEMS)
		return -1;

	len = os_strlen(pos);
	if (len & 1)
		return -1;
	len /= 2;

	if (len == 0) {
		/* Clear subelement */
		e = NULL;
		wpa_printf(MSG_DEBUG, "WFD: Clear subelement %d", subelem);
	} else {
		e = wpabuf_alloc(1 + len);
		if (e == NULL)
			return -1;
		wpabuf_put_u8(e, subelem);
		if (hexstr2bin(pos, wpabuf_put(e, len), len) < 0) {
			wpabuf_free(e);
			return -1;
		}
		wpa_printf(MSG_DEBUG, "WFD: Set subelement %d", subelem);
	}

	wpabuf_free(global->wfd_subelem[subelem]);
	global->wfd_subelem[subelem] = e;
	wifi_display_update_wfd_ie(global);

	return 0;
}


int wifi_display_subelem_get(struct wpa_global *global, char *cmd,
			     char *buf, size_t buflen)
{
	int subelem;

	subelem = atoi(cmd);
	if (subelem < 0 || subelem >= MAX_WFD_SUBELEMS)
		return -1;

	if (global->wfd_subelem[subelem] == NULL)
		return 0;

	return wpa_snprintf_hex(buf, buflen,
				wpabuf_head_u8(global->wfd_subelem[subelem]) +
				1,
				wpabuf_len(global->wfd_subelem[subelem]) - 1);
}


