/*
 * WPA Supplicant
 * Copyright (c) 2003-2009, Jouni Malinen <j@w1.fi>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * Alternatively, this software may be distributed under the terms of BSD
 * license.
 *
 * See README and COPYING for more details.
 *
 * This file implements functions for registering and unregistering
 * %wpa_supplicant interfaces. In addition, this file contains number of
 * functions for managing network connections.
 */

#include "includes.h"

#include "common.h"
#include "eapol_supp/eapol_supp_sm.h"
#include "eap_peer/eap.h"
#include "wpa.h"
#include "eloop.h"
#include "drivers/driver.h"
#include "config.h"
#include "l2_packet/l2_packet.h"
#include "wpa_supplicant_i.h"
#include "ctrl_iface.h"
#include "ctrl_iface_dbus.h"

#ifdef MTK_EAP_SIM_AKA
#include "rild_porting.h"
#else
#include "pcsc_funcs.h"
#endif

#include "version.h"
#include "preauth.h"
#include "pmksa_cache.h"
#include "wpa_ctrl.h"
#include "mlme.h"
#include "ieee802_11_defs.h"
#include "blacklist.h"
#include "wpas_glue.h"
#include "wps_supplicant.h"
#ifdef ANDROID
#include <cutils/properties.h>
#endif

#ifdef  CONFIG_WAPI_SUPPORT
#include "../wapi/interface_inout.h"
#define WAPI_GET_CERT
#ifdef WAPI_GET_CERT
#include <stdio.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>

#include <cutils/sockets.h>
#else
#include "keystore_get.h"
#endif

#endif

const char *wpa_supplicant_version =
"wpa_supplicant v" VERSION_STR "\n"
"Copyright (c) 2003-2009, Jouni Malinen <j@w1.fi> and contributors";

const char *wpa_supplicant_license =
"This program is free software. You can distribute it and/or modify it\n"
"under the terms of the GNU General Public License version 2.\n"
"\n"
"Alternatively, this software may be distributed under the terms of the\n"
"BSD license. See README and COPYING for more details.\n"
#ifdef EAP_TLS_OPENSSL
"\nThis product includes software developed by the OpenSSL Project\n"
"for use in the OpenSSL Toolkit (http://www.openssl.org/)\n"
#endif /* EAP_TLS_OPENSSL */
;

#ifndef CONFIG_NO_STDOUT_DEBUG
/* Long text divided into parts in order to fit in C89 strings size limits. */
const char *wpa_supplicant_full_license1 =
"This program is free software; you can redistribute it and/or modify\n"
"it under the terms of the GNU General Public License version 2 as\n"
"published by the Free Software Foundation.\n"
"\n"
"This program is distributed in the hope that it will be useful,\n"
"but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
"GNU General Public License for more details.\n"
"\n";
const char *wpa_supplicant_full_license2 =
"You should have received a copy of the GNU General Public License\n"
"along with this program; if not, write to the Free Software\n"
"Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA\n"
"\n"
"Alternatively, this software may be distributed under the terms of the\n"
"BSD license.\n"
"\n"
"Redistribution and use in source and binary forms, with or without\n"
"modification, are permitted provided that the following conditions are\n"
"met:\n"
"\n";
const char *wpa_supplicant_full_license3 =
"1. Redistributions of source code must retain the above copyright\n"
"   notice, this list of conditions and the following disclaimer.\n"
"\n"
"2. Redistributions in binary form must reproduce the above copyright\n"
"   notice, this list of conditions and the following disclaimer in the\n"
"   documentation and/or other materials provided with the distribution.\n"
"\n";
const char *wpa_supplicant_full_license4 =
"3. Neither the name(s) of the above-listed copyright holder(s) nor the\n"
"   names of its contributors may be used to endorse or promote products\n"
"   derived from this software without specific prior written permission.\n"
"\n"
"THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS\n"
"\"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT\n"
"LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR\n"
"A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT\n";
const char *wpa_supplicant_full_license5 =
"OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,\n"
"SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT\n"
"LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n"
"DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n"
"THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n"
"(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
"OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
"\n";
#endif /* CONFIG_NO_STDOUT_DEBUG */

extern int wpa_debug_level;
extern int wpa_debug_show_keys;
extern int wpa_debug_timestamp;

/* Configure default/group WEP keys for static WEP */
static int wpa_set_wep_keys(struct wpa_supplicant *wpa_s,
			    struct wpa_ssid *ssid)
{
	int i, set = 0;

	for (i = 0; i < NUM_WEP_KEYS; i++) {
		if (ssid->wep_key_len[i] == 0)
			continue;

		set = 1;
		wpa_drv_set_key(wpa_s, WPA_ALG_WEP,
				(u8 *) "\xff\xff\xff\xff\xff\xff",
				i, i == ssid->wep_tx_keyidx, (u8 *) "", 0,
				ssid->wep_key[i], ssid->wep_key_len[i]);
	}

	return set;
}


static int wpa_supplicant_set_wpa_none_key(struct wpa_supplicant *wpa_s,
					   struct wpa_ssid *ssid)
{
	u8 key[32];
	size_t keylen;
	wpa_alg alg;
	u8 seq[6] = { 0 };

	/* IBSS/WPA-None uses only one key (Group) for both receiving and
	 * sending unicast and multicast packets. */

	if (ssid->mode != IEEE80211_MODE_IBSS) {
		wpa_printf(MSG_INFO, "WPA: Invalid mode %d (not IBSS/ad-hoc) "
			   "for WPA-None", ssid->mode);
		return -1;
	}

	if (!ssid->psk_set) {
		wpa_printf(MSG_INFO, "WPA: No PSK configured for WPA-None");
		return -1;
	}

	switch (wpa_s->group_cipher) {
	case WPA_CIPHER_CCMP:
		os_memcpy(key, ssid->psk, 16);
		keylen = 16;
		alg = WPA_ALG_CCMP;
		break;
	case WPA_CIPHER_TKIP:
		/* WPA-None uses the same Michael MIC key for both TX and RX */
		os_memcpy(key, ssid->psk, 16 + 8);
		os_memcpy(key + 16 + 8, ssid->psk + 16, 8);
		keylen = 32;
		alg = WPA_ALG_TKIP;
		break;
	default:
		wpa_printf(MSG_INFO, "WPA: Invalid group cipher %d for "
			   "WPA-None", wpa_s->group_cipher);
		return -1;
	}

	/* TODO: should actually remember the previously used seq#, both for TX
	 * and RX from each STA.. */

	return wpa_drv_set_key(wpa_s, alg, (u8 *) "\xff\xff\xff\xff\xff\xff",
			       0, 1, seq, 6, key, keylen);
}


static void wpa_supplicant_timeout(void *eloop_ctx, void *timeout_ctx)
{
	struct wpa_supplicant *wpa_s = eloop_ctx;
	const u8 *bssid = wpa_s->bssid;
	if (is_zero_ether_addr(bssid))
		bssid = wpa_s->pending_bssid;
	if (!is_zero_ether_addr(bssid)) {
		wpa_msg(wpa_s, MSG_INFO, "Authentication with " MACSTR " timed out.",
			MAC2STR(bssid));
		wpa_blacklist_add(wpa_s, bssid);
		wpa_sm_notify_disassoc(wpa_s->wpa);
		os_memcpy(wpa_s->bssid, bssid, ETH_ALEN); /* CR119903, setting disassoc to make driver clear ssid */
		wpa_supplicant_disassociate(wpa_s, WLAN_REASON_DEAUTH_LEAVING);
		wpa_s->reassociate = 1;
		wpa_supplicant_req_scan(wpa_s, 0, 0);
	}
}


/**
 * wpa_supplicant_req_auth_timeout - Schedule a timeout for authentication
 * @wpa_s: Pointer to wpa_supplicant data
 * @sec: Number of seconds after which to time out authentication
 * @usec: Number of microseconds after which to time out authentication
 *
 * This function is used to schedule a timeout for the current authentication
 * attempt.
 */
void wpa_supplicant_req_auth_timeout(struct wpa_supplicant *wpa_s,
				     int sec, int usec)
{
	if (wpa_s->conf && wpa_s->conf->ap_scan == 0 &&
	    wpa_s->driver && IS_WIRED(wpa_s->driver))
		return;

	wpa_msg(wpa_s, MSG_DEBUG, "Setting authentication timeout: %d sec "
		"%d usec", sec, usec);
	eloop_cancel_timeout(wpa_supplicant_timeout, wpa_s, NULL);
	eloop_register_timeout(sec, usec, wpa_supplicant_timeout, wpa_s, NULL);
}


/**
 * wpa_supplicant_cancel_auth_timeout - Cancel authentication timeout
 * @wpa_s: Pointer to wpa_supplicant data
 *
 * This function is used to cancel authentication timeout scheduled with
 * wpa_supplicant_req_auth_timeout() and it is called when authentication has
 * been completed.
 */
void wpa_supplicant_cancel_auth_timeout(struct wpa_supplicant *wpa_s)
{
	wpa_msg(wpa_s, MSG_DEBUG, "Cancelling authentication timeout");
	eloop_cancel_timeout(wpa_supplicant_timeout, wpa_s, NULL);
	wpa_blacklist_del(wpa_s, wpa_s->bssid);
}


/**
 * wpa_supplicant_initiate_eapol - Configure EAPOL state machine
 * @wpa_s: Pointer to wpa_supplicant data
 *
 * This function is used to configure EAPOL state machine based on the selected
 * authentication mode.
 */
void wpa_supplicant_initiate_eapol(struct wpa_supplicant *wpa_s)
{
#ifdef IEEE8021X_EAPOL
	struct eapol_config eapol_conf;
	struct wpa_ssid *ssid = wpa_s->current_ssid;

	eapol_sm_notify_eap_success(wpa_s->eapol, FALSE);
	eapol_sm_notify_eap_fail(wpa_s->eapol, FALSE);

	if (wpa_s->key_mgmt == WPA_KEY_MGMT_NONE ||
	    wpa_s->key_mgmt == WPA_KEY_MGMT_WPA_NONE)
		eapol_sm_notify_portControl(wpa_s->eapol, ForceAuthorized);
	else
		eapol_sm_notify_portControl(wpa_s->eapol, Auto);

	os_memset(&eapol_conf, 0, sizeof(eapol_conf));
	if (wpa_s->key_mgmt == WPA_KEY_MGMT_IEEE8021X_NO_WPA) {
		eapol_conf.accept_802_1x_keys = 1;
		eapol_conf.required_keys = 0;
		if (ssid->eapol_flags & EAPOL_FLAG_REQUIRE_KEY_UNICAST) {
			eapol_conf.required_keys |= EAPOL_REQUIRE_KEY_UNICAST;
		}
		if (ssid->eapol_flags & EAPOL_FLAG_REQUIRE_KEY_BROADCAST) {
			eapol_conf.required_keys |=
				EAPOL_REQUIRE_KEY_BROADCAST;
		}

		if (wpa_s->conf && wpa_s->driver && IS_WIRED(wpa_s->driver)) {
			eapol_conf.required_keys = 0;
		}
	}
	if (wpa_s->conf)
		eapol_conf.fast_reauth = wpa_s->conf->fast_reauth;
	eapol_conf.workaround = ssid->eap_workaround;
	eapol_conf.eap_disabled =
		!wpa_key_mgmt_wpa_ieee8021x(wpa_s->key_mgmt) &&
		wpa_s->key_mgmt != WPA_KEY_MGMT_IEEE8021X_NO_WPA &&
		wpa_s->key_mgmt != WPA_KEY_MGMT_WPS;
	eapol_sm_notify_config(wpa_s->eapol, &ssid->eap, &eapol_conf);
#endif /* IEEE8021X_EAPOL */
}


/**
 * wpa_supplicant_set_non_wpa_policy - Set WPA parameters to non-WPA mode
 * @wpa_s: Pointer to wpa_supplicant data
 * @ssid: Configuration data for the network
 *
 * This function is used to configure WPA state machine and related parameters
 * to a mode where WPA is not enabled. This is called as part of the
 * authentication configuration when the selected network does not use WPA.
 */
void wpa_supplicant_set_non_wpa_policy(struct wpa_supplicant *wpa_s,
				       struct wpa_ssid *ssid)
{
	int i;

	if (ssid->key_mgmt & WPA_KEY_MGMT_WPS)
		wpa_s->key_mgmt = WPA_KEY_MGMT_WPS;
	else if (ssid->key_mgmt & WPA_KEY_MGMT_IEEE8021X_NO_WPA)
		wpa_s->key_mgmt = WPA_KEY_MGMT_IEEE8021X_NO_WPA;
	else
		wpa_s->key_mgmt = WPA_KEY_MGMT_NONE;
	wpa_sm_set_ap_wpa_ie(wpa_s->wpa, NULL, 0);
	wpa_sm_set_ap_rsn_ie(wpa_s->wpa, NULL, 0);
	wpa_sm_set_assoc_wpa_ie(wpa_s->wpa, NULL, 0);
	wpa_s->pairwise_cipher = WPA_CIPHER_NONE;
	wpa_s->group_cipher = WPA_CIPHER_NONE;
	wpa_s->mgmt_group_cipher = 0;

	for (i = 0; i < NUM_WEP_KEYS; i++) {
		if (ssid->wep_key_len[i] > 5) {
			wpa_s->pairwise_cipher = WPA_CIPHER_WEP104;
			wpa_s->group_cipher = WPA_CIPHER_WEP104;
			break;
		} else if (ssid->wep_key_len[i] > 0) {
			wpa_s->pairwise_cipher = WPA_CIPHER_WEP40;
			wpa_s->group_cipher = WPA_CIPHER_WEP40;
			break;
		}
	}

	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_RSN_ENABLED, 0);
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_KEY_MGMT, wpa_s->key_mgmt);
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_PAIRWISE,
			 wpa_s->pairwise_cipher);
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_GROUP, wpa_s->group_cipher);
#ifdef CONFIG_IEEE80211W
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_MGMT_GROUP,
			 wpa_s->mgmt_group_cipher);
#endif /* CONFIG_IEEE80211W */

	pmksa_cache_clear_current(wpa_s->wpa);
}


static void wpa_supplicant_cleanup(struct wpa_supplicant *wpa_s)
{
	scard_deinit(wpa_s->scard);
	wpa_s->scard = NULL;
	wpa_sm_set_scard_ctx(wpa_s->wpa, NULL);
	eapol_sm_register_scard_ctx(wpa_s->eapol, NULL);
	l2_packet_deinit(wpa_s->l2);
	wpa_s->l2 = NULL;
	if (wpa_s->l2_br) {
		l2_packet_deinit(wpa_s->l2_br);
		wpa_s->l2_br = NULL;
	}

	if (wpa_s->ctrl_iface) {
		wpa_supplicant_ctrl_iface_deinit(wpa_s->ctrl_iface);
		wpa_s->ctrl_iface = NULL;
	}
	if (wpa_s->conf != NULL) {
		wpa_config_free(wpa_s->conf);
		wpa_s->conf = NULL;
	}

	os_free(wpa_s->confname);
	wpa_s->confname = NULL;

	wpa_sm_set_eapol(wpa_s->wpa, NULL);
	eapol_sm_deinit(wpa_s->eapol);
	wpa_s->eapol = NULL;

	rsn_preauth_deinit(wpa_s->wpa);

	pmksa_candidate_free(wpa_s->wpa);

#ifdef CONFIG_WAPI_SUPPORT
	wpa_supplicant_deinit_wapi(wpa_s);
#endif

	wpa_sm_deinit(wpa_s->wpa);
	wpa_s->wpa = NULL;
	wpa_blacklist_clear(wpa_s);

	wpa_scan_results_free(wpa_s->scan_res);
	wpa_s->scan_res = NULL;

	wpa_supplicant_cancel_scan(wpa_s);
	wpa_supplicant_cancel_auth_timeout(wpa_s);

	ieee80211_sta_deinit(wpa_s);

	wpas_wps_deinit(wpa_s);

	wpabuf_free(wpa_s->pending_eapol_rx);
	wpa_s->pending_eapol_rx = NULL;
}


/**
 * wpa_clear_keys - Clear keys configured for the driver
 * @wpa_s: Pointer to wpa_supplicant data
 * @addr: Previously used BSSID or %NULL if not available
 *
 * This function clears the encryption keys that has been previously configured
 * for the driver.
 */
void wpa_clear_keys(struct wpa_supplicant *wpa_s, const u8 *addr)
{
	u8 *bcast = (u8 *) "\xff\xff\xff\xff\xff\xff";

	if (wpa_s->keys_cleared) {
		/* Some drivers (e.g., ndiswrapper & NDIS drivers) seem to have
		 * timing issues with keys being cleared just before new keys
		 * are set or just after association or something similar. This
		 * shows up in group key handshake failing often because of the
		 * client not receiving the first encrypted packets correctly.
		 * Skipping some of the extra key clearing steps seems to help
		 * in completing group key handshake more reliably. */
		wpa_printf(MSG_DEBUG, "No keys have been configured - "
			   "skip key clearing");
		return;
	}

	/* MLME-DELETEKEYS.request */
	wpa_drv_set_key(wpa_s, WPA_ALG_NONE, bcast, 0, 0, NULL, 0, NULL, 0);
	wpa_drv_set_key(wpa_s, WPA_ALG_NONE, bcast, 1, 0, NULL, 0, NULL, 0);
	wpa_drv_set_key(wpa_s, WPA_ALG_NONE, bcast, 2, 0, NULL, 0, NULL, 0);
	wpa_drv_set_key(wpa_s, WPA_ALG_NONE, bcast, 3, 0, NULL, 0, NULL, 0);
#ifdef CONFIG_IEEE80211W
	wpa_drv_set_key(wpa_s, WPA_ALG_NONE, bcast, 4, 0, NULL, 0, NULL, 0);
	wpa_drv_set_key(wpa_s, WPA_ALG_NONE, bcast, 5, 0, NULL, 0, NULL, 0);
#endif /* CONFIG_IEEE80211W */
	if (addr) {
		wpa_drv_set_key(wpa_s, WPA_ALG_NONE, addr, 0, 0, NULL, 0, NULL,
				0);
		/* MLME-SETPROTECTION.request(None) */
		wpa_drv_mlme_setprotection(
			wpa_s, addr,
			MLME_SETPROTECTION_PROTECT_TYPE_NONE,
			MLME_SETPROTECTION_KEY_TYPE_PAIRWISE);
	}
	wpa_s->keys_cleared = 1;
}


/**
 * wpa_supplicant_state_txt - Get the connection state name as a text string
 * @state: State (wpa_state; WPA_*)
 * Returns: The state name as a printable text string
 */
const char * wpa_supplicant_state_txt(int state)
{
	switch (state) {
	case WPA_DISCONNECTED:
		return "DISCONNECTED";
	case WPA_INACTIVE:
		return "INACTIVE";
#if ICS_LEGACY_WLAN_SUPPORT
	case WPA_INTERFACE_DISABLED:
		return "INTERFACE_DISABLED";
#endif
	case WPA_SCANNING:
		return "SCANNING";
#if ICS_LEGACY_WLAN_SUPPORT
	case WPA_AUTHENTICATING:
		return "AUTHENTICATING";
#endif
	case WPA_ASSOCIATING:
		return "ASSOCIATING";
	case WPA_ASSOCIATED:
		return "ASSOCIATED";
	case WPA_4WAY_HANDSHAKE:
		return "4WAY_HANDSHAKE";
	case WPA_GROUP_HANDSHAKE:
		return "GROUP_HANDSHAKE";
	case WPA_COMPLETED:
		return "COMPLETED";
	default:
		return "UNKNOWN";
	}
}


/**
 * wpa_supplicant_set_state - Set current connection state
 * @wpa_s: Pointer to wpa_supplicant data
 * @state: The new connection state
 *
 * This function is called whenever the connection state changes, e.g.,
 * association is completed for WPA/WPA2 4-Way Handshake is started.
 */
void wpa_supplicant_set_state(struct wpa_supplicant *wpa_s, wpa_states state)
{
#ifdef ANDROID
	int network_id = -1;
    /* BWCS add */
#ifdef MTK_WLANBT_SINGLEANT
    static int fgconn = 0;
    if (state == WPA_DISCONNECTED && fgconn == 1)
    {
        if(bwcs_send_internal_event(0x0A) < 0){ //discon
        wpa_msg(wpa_s, MSG_INFO, "bwcs wpa_supplicant discon socket error!");
        	}
		fgconn = 0; //reset conn state
	wpa_msg(wpa_s, MSG_INFO, "bwcs wpa_supplicant discon");
    }
    else if (state == WPA_COMPLETED && fgconn == 0)
    {
        if(bwcs_send_internal_event(0x09) < 0){// conn
        wpa_msg(wpa_s, MSG_INFO, "bwcs wpa_supplicant con socket error");
        	}
        fgconn = 1; //record conn
        wpa_msg(wpa_s, MSG_INFO, "bwcs wpa_supplicant con");
    }
#endif
	if (wpa_s && wpa_s->current_ssid) {
		network_id = wpa_s->current_ssid->id;
	}
	wpa_states reported_state = state;
	if (state == WPA_DISCONNECTED && wpa_s->disconnected) {
		reported_state = WPA_IDLE;
	}
#endif
	wpa_printf(MSG_DEBUG, "State: %s -> %s",
		   wpa_supplicant_state_txt(wpa_s->wpa_state),
		   wpa_supplicant_state_txt(state));

	if (state != WPA_SCANNING)
		wpa_supplicant_notify_scanning(wpa_s, 0);

	wpa_supplicant_dbus_notify_state_change(wpa_s, state,
						wpa_s->wpa_state);
#ifdef ANDROID
	if (wpa_s->current_ssid){
		if(!(wpa_s->current_ssid->key_mgmt & WPA_KEY_MGMT_WPS)){
	wpa_msg(wpa_s, MSG_INFO, WPA_EVENT_STATE_CHANGE "id=%d state=%d BSSID=" MACSTR " SSID=%s",
		    network_id, reported_state, MAC2STR(wpa_s->pending_bssid),
		    wpa_s->current_ssid && wpa_s->current_ssid->ssid ? wpa_ssid_txt(wpa_s->current_ssid->ssid, wpa_s->current_ssid->ssid_len) : ""
		    );
		}
	}
	else{
	wpa_msg(wpa_s, MSG_INFO, WPA_EVENT_STATE_CHANGE "id=%d state=%d BSSID=" MACSTR " SSID=%s",
		    network_id, reported_state, MAC2STR(wpa_s->pending_bssid),
		    wpa_s->current_ssid && wpa_s->current_ssid->ssid ? wpa_ssid_txt(wpa_s->current_ssid->ssid, wpa_s->current_ssid->ssid_len) : ""
		    );
	}
#endif

	if (state == WPA_COMPLETED && wpa_s->new_connection) {
#if defined(CONFIG_CTRL_IFACE) || !defined(CONFIG_NO_STDOUT_DEBUG)
		struct wpa_ssid *ssid = wpa_s->current_ssid;
		wpa_msg(wpa_s, MSG_INFO, WPA_EVENT_CONNECTED "- Connection to "
			MACSTR " completed %s [id=%d id_str=%s]",
			MAC2STR(wpa_s->bssid), wpa_s->reassociated_connection ?
			"(reauth)" : "(auth)",
			ssid ? ssid->id : -1,
			ssid && ssid->id_str ? ssid->id_str : "");
#endif /* CONFIG_CTRL_IFACE || !CONFIG_NO_STDOUT_DEBUG */
		wpa_s->new_connection = 0;
		wpa_s->reassociated_connection = 1;
		wpa_drv_set_operstate(wpa_s, 1);
	} else if (state == WPA_DISCONNECTED || state == WPA_ASSOCIATING ||
		   state == WPA_ASSOCIATED) {
		wpa_s->new_connection = 1;
		wpa_drv_set_operstate(wpa_s, 0);
	}
	wpa_s->wpa_state = state;
}


static void wpa_supplicant_terminate(int sig, void *eloop_ctx,
				     void *signal_ctx)
{
	struct wpa_global *global = eloop_ctx;
	struct wpa_supplicant *wpa_s;
	for (wpa_s = global->ifaces; wpa_s; wpa_s = wpa_s->next) {
		wpa_msg(wpa_s, MSG_INFO, WPA_EVENT_TERMINATING "- signal %d "
			"received", sig);
	}
	eloop_terminate();
}


static void wpa_supplicant_clear_status(struct wpa_supplicant *wpa_s)
{
#if ICS_LEGACY_WLAN_SUPPORT
	wpa_states old_state = wpa_s->wpa_state;
#endif

	wpa_s->pairwise_cipher = 0;
	wpa_s->group_cipher = 0;
	wpa_s->mgmt_group_cipher = 0;
	wpa_s->key_mgmt = 0;
#if ICS_LEGACY_WLAN_SUPPORT
	if (wpa_s->wpa_state != WPA_INTERFACE_DISABLED)
		wpa_s->wpa_state = WPA_DISCONNECTED;

	if (wpa_s->wpa_state != old_state) {
#ifdef ANDROID
	wpa_msg_ctrl(wpa_s, MSG_INFO, WPA_EVENT_STATE_CHANGE "id=%d state=%d BSSID=" MACSTR " SSID=%s",
		    wpa_s->current_ssid ? wpa_s->current_ssid->id : -1, wpa_s->wpa_state, MAC2STR(wpa_s->pending_bssid),
		    wpa_s->current_ssid && wpa_s->current_ssid->ssid ? wpa_ssid_txt(wpa_s->current_ssid->ssid, wpa_s->current_ssid->ssid_len) : ""
		    );
#endif /* ANDROID */
	}
#else
	wpa_s->wpa_state = WPA_DISCONNECTED;
#endif

}


/**
 * wpa_supplicant_reload_configuration - Reload configuration data
 * @wpa_s: Pointer to wpa_supplicant data
 * Returns: 0 on success or -1 if configuration parsing failed
 *
 * This function can be used to request that the configuration data is reloaded
 * (e.g., after configuration file change). This function is reloading
 * configuration only for one interface, so this may need to be called multiple
 * times if %wpa_supplicant is controlling multiple interfaces and all
 * interfaces need reconfiguration.
 */
int wpa_supplicant_reload_configuration(struct wpa_supplicant *wpa_s)
{
	struct wpa_config *conf;
	int reconf_ctrl;
	if (wpa_s->confname == NULL)
		return -1;
	conf = wpa_config_read(wpa_s->confname);
	if (conf == NULL) {
		wpa_msg(wpa_s, MSG_ERROR, "Failed to parse the configuration "
			"file '%s' - exiting", wpa_s->confname);
		return -1;
	}

	reconf_ctrl = !!conf->ctrl_interface != !!wpa_s->conf->ctrl_interface
		|| (conf->ctrl_interface && wpa_s->conf->ctrl_interface &&
		    os_strcmp(conf->ctrl_interface,
			      wpa_s->conf->ctrl_interface) != 0);

	if (reconf_ctrl && wpa_s->ctrl_iface) {
		wpa_supplicant_ctrl_iface_deinit(wpa_s->ctrl_iface);
		wpa_s->ctrl_iface = NULL;
	}

	eapol_sm_invalidate_cached_session(wpa_s->eapol);
	wpa_s->current_ssid = NULL;
	/*
	 * TODO: should notify EAPOL SM about changes in opensc_engine_path,
	 * pkcs11_engine_path, pkcs11_module_path.
	 */
	if (wpa_key_mgmt_wpa_psk(wpa_s->key_mgmt)) {
		/*
		 * Clear forced success to clear EAP state for next
		 * authentication.
		 */
		eapol_sm_notify_eap_success(wpa_s->eapol, FALSE);
	}
	eapol_sm_notify_config(wpa_s->eapol, NULL, NULL);
	wpa_sm_set_config(wpa_s->wpa, NULL);
	wpa_sm_set_fast_reauth(wpa_s->wpa, wpa_s->conf->fast_reauth);
	rsn_preauth_deinit(wpa_s->wpa);
	wpa_config_free(wpa_s->conf);
	wpa_s->conf = conf;
	if (reconf_ctrl)
		wpa_s->ctrl_iface = wpa_supplicant_ctrl_iface_init(wpa_s);

	wpa_s->wps->dev.device_name = wpa_s->conf->device_name;
	wpa_s->wps->dev.manufacturer = wpa_s->conf->manufacturer;
	wpa_s->wps->dev.model_name = wpa_s->conf->model_name;
	wpa_s->wps->dev.model_number = wpa_s->conf->model_number;
	wpa_s->wps->dev.serial_number = wpa_s->conf->serial_number;
	wpa_s->wps->config_methods =
		wps_config_methods_str2bin(wpa_s->conf->config_methods);
	if ((wpa_s->wps->config_methods & (WPS_CONFIG_DISPLAY | WPS_CONFIG_LABEL)) ==
		(WPS_CONFIG_DISPLAY | WPS_CONFIG_LABEL)) {
		wpa_printf(MSG_ERROR, "WPS: Both Label and Display config "
			   "methods are not allowed at the same time");
		os_free(wpa_s->wps);
		return -1;
	}
	wpa_s->wps->config_methods = wps_fix_config_methods(wpa_s->wps->config_methods);
	if (wpa_s->conf->device_type) {
		char *pos;
		u8 oui[4];
		/* <categ>-<OUI>-<subcateg> */
		wpa_s->wps->dev.categ = atoi(wpa_s->conf->device_type);
		pos = os_strchr(wpa_s->conf->device_type, '-');
		if (pos == NULL) {
			wpa_printf(MSG_ERROR, "WPS: Invalid device_type");
			os_free(wpa_s->wps);
			return -1;
		}
		pos++;
		if (hexstr2bin(pos, oui, 4)) {
			wpa_printf(MSG_ERROR, "WPS: Invalid device_type OUI");
			os_free(wpa_s->wps);
			return -1;
		}
		wpa_s->wps->dev.oui = WPA_GET_BE32(oui);
		pos = os_strchr(pos, '-');
		if (pos == NULL) {
			wpa_printf(MSG_ERROR, "WPS: Invalid device_type");
			os_free(wpa_s->wps);
			return -1;
		}
		pos++;
		wpa_s->wps->dev.sub_categ = atoi(pos);
	}
	wpa_s->wps->dev.os_version = WPA_GET_BE32(wpa_s->conf->os_version);
	wpa_s->wps->dev.rf_bands = WPS_RF_24GHZ | WPS_RF_50GHZ; /* TODO: config */
	os_memcpy(wpa_s->wps->dev.mac_addr, wpa_s->own_addr, ETH_ALEN);
	if (is_nil_uuid(wpa_s->conf->uuid)) {
		uuid_gen_mac_addr(wpa_s->own_addr, wpa_s->wps->uuid);
		wpa_hexdump(MSG_DEBUG, "WPS: UUID based on MAC address",
				wpa_s->wps->uuid, WPS_UUID_LEN);
	} else
		os_memcpy(wpa_s->wps->uuid, wpa_s->conf->uuid, WPS_UUID_LEN);

	wpa_supplicant_clear_status(wpa_s);
	wpa_s->reassociate = 1;
#ifdef ANDROID
	wpa_supplicant_req_scan(wpa_s, 2, 0);
#else
	wpa_supplicant_req_scan(wpa_s, 0, 0);
#endif
	wpa_msg(wpa_s, MSG_DEBUG, "Reconfiguration completed");
	return 0;
}


static void wpa_supplicant_reconfig(int sig, void *eloop_ctx,
				    void *signal_ctx)
{
	struct wpa_global *global = eloop_ctx;
	struct wpa_supplicant *wpa_s;
	wpa_printf(MSG_DEBUG, "Signal %d received - reconfiguring", sig);
	for (wpa_s = global->ifaces; wpa_s; wpa_s = wpa_s->next) {
		if (wpa_supplicant_reload_configuration(wpa_s) < 0) {
			eloop_terminate();
		}
	}
}


static wpa_cipher cipher_suite2driver(int cipher)
{
	switch (cipher) {
	case WPA_CIPHER_NONE:
		return CIPHER_NONE;
	case WPA_CIPHER_WEP40:
		return CIPHER_WEP40;
	case WPA_CIPHER_WEP104:
		return CIPHER_WEP104;
	case WPA_CIPHER_CCMP:
		return CIPHER_CCMP;
#if 0
//#ifdef CONFIG_WAPI_SUPPORT
    case WAPI_CIPHER_SMS4:
        return CIPHER_SMS4;
#endif
	case WPA_CIPHER_TKIP:
	default:
		return CIPHER_TKIP;
	}
}


static wpa_key_mgmt key_mgmt2driver(int key_mgmt)
{
	switch (key_mgmt) {
	case WPA_KEY_MGMT_NONE:
		return KEY_MGMT_NONE;
	case WPA_KEY_MGMT_IEEE8021X_NO_WPA:
		return KEY_MGMT_802_1X_NO_WPA;
	case WPA_KEY_MGMT_IEEE8021X:
		return KEY_MGMT_802_1X;
	case WPA_KEY_MGMT_WPA_NONE:
		return KEY_MGMT_WPA_NONE;
#if 0
//#ifdef CONFIG_WAPI_SUPPORT
    case WAPI_KEY_MGMT_PSK:
        return KEY_MGMT_WAPI_PSK;
    case WAPI_KEY_MGMT_CERT:
        return KEY_MGMT_WAPI_CERT;
#endif
	case WPA_KEY_MGMT_FT_IEEE8021X:
		return KEY_MGMT_FT_802_1X;
	case WPA_KEY_MGMT_FT_PSK:
		return KEY_MGMT_FT_PSK;
	case WPA_KEY_MGMT_IEEE8021X_SHA256:
		return KEY_MGMT_802_1X_SHA256;
	case WPA_KEY_MGMT_PSK_SHA256:
		return KEY_MGMT_PSK_SHA256;
	case WPA_KEY_MGMT_WPS:
		return KEY_MGMT_WPS;
	case WPA_KEY_MGMT_PSK:
	default:
		return KEY_MGMT_PSK;
	}
}


static int wpa_supplicant_suites_from_ai(struct wpa_supplicant *wpa_s,
					 struct wpa_ssid *ssid,
					 struct wpa_ie_data *ie)
{
	int ret = wpa_sm_parse_own_wpa_ie(wpa_s->wpa, ie);
	if (ret) {
		if (ret == -2) {
			wpa_msg(wpa_s, MSG_INFO, "WPA: Failed to parse WPA IE "
				"from association info");
		}
		return -1;
	}

	#ifdef CONFIG_WAPI_SUPPORT
	if (ssid->proto & WPA_PROTO_WAPI){
			wpa_msg(wpa_s, MSG_INFO, "[WAPI-Debug]WPA: No support wpa_supplicant_suites_from_ai"
				" for WAPI");
			return -1;
	}
	#endif

	wpa_printf(MSG_DEBUG, "WPA: Using WPA IE from AssocReq to set cipher "
		   "suites");
	if (!(ie->group_cipher & ssid->group_cipher)) {
		wpa_msg(wpa_s, MSG_INFO, "WPA: Driver used disabled group "
			"cipher 0x%x (mask 0x%x) - reject",
			ie->group_cipher, ssid->group_cipher);
		return -1;
	}
	if (!(ie->pairwise_cipher & ssid->pairwise_cipher)) {
		wpa_msg(wpa_s, MSG_INFO, "WPA: Driver used disabled pairwise "
			"cipher 0x%x (mask 0x%x) - reject",
			ie->pairwise_cipher, ssid->pairwise_cipher);
		return -1;
	}
	if (!(ie->key_mgmt & ssid->key_mgmt)) {
		wpa_msg(wpa_s, MSG_INFO, "WPA: Driver used disabled key "
			"management 0x%x (mask 0x%x) - reject",
			ie->key_mgmt, ssid->key_mgmt);
		return -1;
	}

#ifdef CONFIG_IEEE80211W
	if (!(ie->capabilities & WPA_CAPABILITY_MFPC) &&
	    ssid->ieee80211w == IEEE80211W_REQUIRED) {
		wpa_msg(wpa_s, MSG_INFO, "WPA: Driver associated with an AP "
			"that does not support management frame protection - "
			"reject");
		return -1;
	}
#endif /* CONFIG_IEEE80211W */

	return 0;
}

#ifdef CONFIG_WAPI_SUPPORT
#ifdef _MSC_VER
#pragma pack(push, 1)
#endif /* _MSC_VER */

struct wapi_ie_hdr {
	u8 elem_id; /* WLAN_EID_RSN */
	u8 len;
	//u8 version[2];
	u16 version;
} STRUCT_PACKED;


static const int WAPI_SELECTOR_LEN = 4;
static const u16 WAPI_VERSION = 1;
static const u8 WAPI_AUTH_KEY_MGMT_WAI[] = { 0x00, 0x14, 0x72, 1 };
static const u8 WAPI_AUTH_KEY_MGMT_PSK[] = { 0x00, 0x14, 0x72, 2 };
static const u8 WAPI_CIPHER_SUITE_WPI[] = { 0x00, 0x14, 0x72, 1 };

static int wapi_selector_to_bitfield(u8 *s)
{
	if (os_memcmp(s, WAPI_CIPHER_SUITE_WPI, WAPI_SELECTOR_LEN) == 0)
		return WAPI_CIPHER_SMS4;
	return 0;
}


int wapi_key_mgmt_to_bitfield(u8 *s)
{
	if (os_memcmp(s, WAPI_AUTH_KEY_MGMT_WAI, WAPI_SELECTOR_LEN) == 0)
		return WAPI_KEY_MGMT_CERT;
	if (os_memcmp(s, WAPI_AUTH_KEY_MGMT_PSK, WAPI_SELECTOR_LEN) ==0)
		return WAPI_KEY_MGMT_PSK;
	return 0;
}

int wpa_gen_wapi_ie(struct wpa_supplicant *wpa_s, u8 *wapi_ie)
{
	u8 *pos;

	struct wapi_ie_hdr *hdr;


	hdr = (struct wapi_ie_hdr *) wapi_ie;
	hdr->elem_id = WAPI_INFO_ELEM;
	hdr->version = host_to_le16(WAPI_VERSION);
	//os_memcpy(hdr->version, &(host_to_le16(WAPI_VERSION)), 2);
	pos = (u8 *) (hdr + 1);

    /* akm count = 1 */
	*pos++ = 1;
	*pos++ = 0;

	if (wpa_s->key_mgmt == WAPI_KEY_MGMT_CERT) {
		os_memcpy(pos, WAPI_AUTH_KEY_MGMT_WAI, WAPI_SELECTOR_LEN);
	} else if (wpa_s->key_mgmt == WAPI_KEY_MGMT_PSK) {
		os_memcpy(pos, WAPI_AUTH_KEY_MGMT_PSK, WAPI_SELECTOR_LEN);
	} else {
		wpa_printf(MSG_ERROR, "Invalid key management type (%d).",
			   wpa_s->key_mgmt);
		return -1;
	}
	pos += WAPI_SELECTOR_LEN;

    /* unicase count = 1 */
	*pos++ = 1;
	*pos++ = 0;

	if (wpa_s->pairwise_cipher == WAPI_CIPHER_SMS4) {
		os_memcpy(pos, WAPI_CIPHER_SUITE_WPI, WAPI_SELECTOR_LEN);
	} else {
		wpa_printf(MSG_ERROR, "Invalid pairwise cipher (%d).",
			   wpa_s->pairwise_cipher);
		return -1;
	}
	pos += WAPI_SELECTOR_LEN;

    /* group cast key count */
	if (wpa_s->group_cipher == WAPI_CIPHER_SMS4) {
		os_memcpy(pos, WAPI_CIPHER_SUITE_WPI, WAPI_SELECTOR_LEN);
	} else {
		wpa_printf(MSG_ERROR, "Invalid group cipher (%d).",
			   wpa_s->group_cipher);
		return -1;
	}
	pos += WAPI_SELECTOR_LEN;

	/* WAPI Capabilities */
	*pos++ = 0;
	*pos++ = 0;

	/* BKID Count (2 octets, little endian) */
	*pos++ = 0;
	*pos++ = 0;


	hdr->len = (pos - wapi_ie) - 2;

	return pos - wapi_ie;
}

int wpa_parse_wapi_ie(const u8 *wapi_ie, size_t wapi_ie_len, struct wpa_ie_data *data)
{
	//rsn->wapi
	struct wapi_ie_hdr *hdr;
	u8 *pos;
	int left;
	int i, count;

	data->proto = WPA_PROTO_WAPI;
	data->pairwise_cipher = WAPI_CIPHER_SMS4;
	data->group_cipher = WAPI_CIPHER_SMS4;
	data->key_mgmt = WAPI_KEY_MGMT_PSK;
	data->capabilities = 0;
	data->pmkid = NULL;
	data->num_pmkid = 0;
	if (wapi_ie_len == 0) {
		/* No WAPI IE - fail silently */
		wpa_printf(MSG_ERROR, "WAPI IE len is 0");
		return -1;
	}
	if (wapi_ie_len < sizeof(struct wapi_ie_hdr)) {
		wpa_printf(MSG_ERROR, "WAPI IE len too short %lu", (unsigned long) wapi_ie_len);
		return -1;
	}

	hdr = (struct wapi_ie_hdr *) wapi_ie;

	if ((hdr->elem_id != WAPI_INFO_ELEM ||
	    hdr->len != wapi_ie_len - 2 ||
	    //check it
	    le_to_host16(hdr->version) != WAPI_VERSION
		/*os_memcmp(&le_to_host16(hdr->version), &WAPI_VERSION, 2*/) !=0){
		wpa_printf(MSG_ERROR, "malformed WAPI IE or unknown version");
		return -1;
	}
	pos = (u8 *) (hdr + 1);
	left = wapi_ie_len - sizeof(*hdr);

    if (left >= 2 ) {
        count = pos[0] | (((u16)pos[1]) << 8);

        if (count != 1 ) {
    		wpa_printf(MSG_ERROR, "WAPI IE invalid AKM count %u", count);
    		return -1;
        }

		pos += 2;
		left -= 2;
    }else{
		wpa_printf(MSG_ERROR, "WAPI IE length mismatch, %u too much", left);
		return -1;
    }

    if (left >= WAPI_SELECTOR_LEN ) {
		data->key_mgmt = wapi_key_mgmt_to_bitfield(pos);
		pos += WAPI_SELECTOR_LEN;
		left -= WAPI_SELECTOR_LEN;
    }else{
		wpa_printf(MSG_ERROR, "WAPI IE length mismatch, %u too much", left);
		return -1;
    }


	if (left >= 2) {
		data->pairwise_cipher = 0;
		count = pos[0] | (((int)pos[1]) << 8);
		pos += 2;
		left -= 2;
		if (count == 0 || left < count * WAPI_SELECTOR_LEN) {
			wpa_printf(MSG_ERROR, "WAPI IE count botch (pairwise), "
				   "count %u left %u", count, left);
			return -1;
		}

		for (i = 0; i < count; i++) {
			data->pairwise_cipher |= wapi_selector_to_bitfield(pos);
			pos += WAPI_SELECTOR_LEN;
			left -= WAPI_SELECTOR_LEN;
		}
	} else if (left == 1) {
		wpa_printf(MSG_ERROR, "WAPI IE too short (for key mgmt)");
		return -1;
	}

	if (left >= WAPI_SELECTOR_LEN) {

		data->group_cipher = wapi_selector_to_bitfield(pos);
		pos += WAPI_SELECTOR_LEN;
		left -= WAPI_SELECTOR_LEN;
	} else if (left > 0) {
		wpa_printf(MSG_ERROR, "WAPI IE length mismatch, %u too much", left);
		return -1;
	}

	if (left >= 2) {
		data->capabilities = pos[0] | (((int)pos[1]) << 8);
		pos += 2;
		left -= 2;
	}

    /* ignore WAPI BKID we do not support anyway. */

	if (left > 0) {
		wpa_printf(MSG_ERROR, "WAPI IE has %u trailing bytes - ignored", left);
	}
	return 0;
}


#ifdef WAPI_GET_CERT
static int wapi_keystore_get(struct wpa_supplicant *wpa_s,const char *key, int length, char *value)
{
    uint8_t bytes[2];
    uint8_t code = 'g';
    int sock;
    int offset = 0;
    int i=0;

enum response_code {
    NO_ERROR          =  1,
    LOCKED            =  2,
    UNINITIALIZED     =  3,
    SYSTEM_ERROR      =  4,
    PROTOCOL_ERROR    =  5,
    PERMISSION_DENIED =  6,
    KEY_NOT_FOUND     =  7,
    VALUE_CORRUPTED   =  8,
    UNDEFINED_ACTION  =  9,
    WRONG_PASSWORD    = 10,
};

char *responses[256] = {
    [NO_ERROR]           = "No error",
    [LOCKED]             = "Locked",
    [UNINITIALIZED]      = "Uninitialized",
    [SYSTEM_ERROR]       = "System error",
    [PROTOCOL_ERROR]     = "Protocol error",
    [PERMISSION_DENIED]  = "Permission denied",
    [KEY_NOT_FOUND]      = "Key not found",
    [VALUE_CORRUPTED]    = "Value corrupted",
    [UNDEFINED_ACTION]   = "Undefined action",
    [WRONG_PASSWORD]     = "Wrong password (last chance)",
    [WRONG_PASSWORD + 1] = "Wrong password (2 tries left)",
    [WRONG_PASSWORD + 2] = "Wrong password (3 tries left)",
    [WRONG_PASSWORD + 3] = "Wrong password (4 tries left)",
};

    if (length < 0 || length > 65535) {
        return -1;
    }

    sock = socket_local_client("keystore", ANDROID_SOCKET_NAMESPACE_RESERVED,
                               SOCK_STREAM);
    if (sock == -1) {
        return 1;
    }

    code = 'g';
    send(sock, &code, 1, 0);
    bytes[0] = length >> 8;
    bytes[1] = length;

    wpa_printf(MSG_INFO,"lenght = %d\n", length);

    send(sock, &bytes[0], 2, 0);
    send(sock, key, length, 0);

    wpa_printf(MSG_INFO,"shutdown wr\n", key);
    shutdown(sock, SHUT_WR);
    if (recv(sock, &code, 1, 0) != 1) {
        wpa_printf(MSG_ERROR,"Failed to receive");
        return 1;
    }
    wpa_printf(MSG_INFO,"[!--WAPI-INFO--] %d %s\n", code , responses[code] ? responses[code] : "Unknown");
    if(code == UNINITIALIZED || code == KEY_NOT_FOUND)
    {
        wpa_msg(wpa_s, MSG_INFO, WPA_EVENT_EAP_NO_CERTIFICATION);

    }

    while ((i = recv(sock, &bytes[0], 1, 0)) == 1) {
        int length;
        int offset;
        if ((i = recv(sock, &bytes[1], 1, 0)) != 1) {
           wpa_printf(MSG_ERROR,"Failed to receive");
            return 1;
        }
        length = bytes[0] << 8 | bytes[1];
        for (offset = 0; offset < length; offset += i) {
            i = recv(sock, &value[offset], length - offset, 0);
            if (i <= 0) {
                wpa_printf(MSG_ERROR,"Failed to receive");
                return 1;
            }
        }
    }

    close(sock);
    return length;
}
#endif

int wpa_supplincant_set_wapi_param(struct wpa_supplicant *wpa_s,
	struct wpa_ssid *ssid)
{
    wapi_param_t  wapi_param;
    int wapi_set_param_result = 0;

    if(wpa_s->key_mgmt == WAPI_KEY_MGMT_PSK){
        /* it's not PMK actually but string */
        wapi_param.authType = AUTH_TYPE_WAPI_PSK;

        wpa_printf(MSG_INFO, "[WAPI-Debug]++++handle WAPI Preshare key case++++\n");

		if(ssid->passphrase){ /*ascii mode*/
			wapi_param.para.kt = KEY_TYPE_ASCII;
			buffer_hexdump(MSG_MSGDUMP,"[WAPI-Debug][wapi-passphrase]",(const u8*) ssid->passphrase, strlen((char*)ssid->passphrase));
			if(!ssid->passphrase){
				wpa_printf(MSG_ERROR, "[WAPI-Debug] ssid->passphrase is NULL , Fatal error %s ",__FUNCTION__);
                return -1;
            }
    		wapi_param.para.kl = strlen((char*)ssid->passphrase);
    		memcpy(wapi_param.para.kv, ssid->passphrase, wapi_param.para.kl);
    		wapi_param.para.kv[wapi_param.para.kl] = '\0';

        }else{/*hex mode*/
			wapi_param.para.kt = KEY_TYPE_HEX;
            wapi_param.para.kl = ssid->len_wapi_hex_psk/2;
            memcpy(wapi_param.para.kv, ssid->wapi_hex_psk, wapi_param.para.kl);
            buffer_hexdump(MSG_MSGDUMP,"[WAPI-Debug][wapi-hex]", wapi_param.para.kv, wapi_param.para.kl);
		}
        /*
		wpa_printf(MSG_DEBUG, "[WAPI-Debug!!!!!*_*!!] auth %d type %d length %d %s",
            (int)wapi_param.authType, (int)wapi_param.para.kt,
            (int)wapi_param.para.kl, wapi_param.para.kv);
		*/
        wapi_set_param_result = wapi_set_user(&wapi_param);

        wpa_printf(MSG_INFO, "[WAPI-Debug]WAPI: WAI_CNTAPPARA_SET result %d",
            wapi_set_param_result);

    }else if(wpa_s->key_mgmt == WAPI_KEY_MGMT_CERT){
        wapi_param.authType = AUTH_TYPE_WAPI;

        wpa_printf(MSG_INFO, "[WAPI-Debug]++++handle WAPI certificate case++++\n");

		/*get certificate from keystore*/
#ifdef WAPI_GET_CERT
        #define KEYSTORE_MESSAGE_SIZE 65535
#endif
		#define WAPI_CERT_FROM_KEYSTORE
        #define WAPI_CERTIFICATE_MAX_SIZE 2048
        #define WAPI_CERTIFICATE_KEY_MAX_LEN 128
		#ifdef WAPI_CERT_FROM_KEYSTORE
		{
			char value[KEYSTORE_MESSAGE_SIZE];
			int length = 0;
            u8 user_cert[WAPI_CERTIFICATE_KEY_MAX_LEN];
            u8 as_cert[WAPI_CERTIFICATE_KEY_MAX_LEN];
            int user_cert_idx_len = 0;
            int as_cert_idx_len = 0;

            if(ssid && ssid->eap.ca_cert2)
                wpa_printf(MSG_DEBUG, "[WAPI-Debug]ca_cert2 = %s\n",ssid->eap.ca_cert2);
            else {
                wpa_printf(MSG_ERROR, "[WAPI-Debug]ca_cert2 = %s\n","NULL" );
                return -1;
            }

            if(ssid && ssid->eap.client_cert)
                wpa_printf(MSG_DEBUG, "[WAPI-Debug]client_cert = %s\n",ssid->eap.client_cert);
            else {
                wpa_printf(MSG_ERROR, "[WAPI-Debug]client_cert  = %s\n","NULL" );
                return -1;
            }

            user_cert_idx_len = os_strlen((char*)ssid->eap.client_cert);
            if(user_cert_idx_len <= WAPI_CERTIFICATE_KEY_MAX_LEN){
                os_memset(&user_cert[0], 0, WAPI_CERTIFICATE_KEY_MAX_LEN);
    		    os_memcpy(&user_cert[0], ssid->eap.client_cert, user_cert_idx_len);
    		    user_cert[user_cert_idx_len] = '\0';
            }else{
                 wpa_printf(MSG_ERROR, "[WAPI-Debug]user_cert_idx_len is too long\n");
                 return -1;
            }

            as_cert_idx_len = os_strlen((char*)ssid->eap.ca_cert2);
            if(as_cert_idx_len <= WAPI_CERTIFICATE_KEY_MAX_LEN){
                os_memset(&as_cert[0], 0, WAPI_CERTIFICATE_KEY_MAX_LEN);
    		    os_memcpy(&as_cert[0], ssid->eap.ca_cert2, as_cert_idx_len);
    		    as_cert[as_cert_idx_len] = '\0';
            }else{
                wpa_printf(MSG_ERROR, "[WAPI-Debug]as_cert_idx_len is too long\n");
                return -1;
            }

			wpa_printf(MSG_DEBUG, "[WAPI-Debug]KEYSTORE_MESSAGE_SIZE = %d", KEYSTORE_MESSAGE_SIZE);
			wpa_printf(MSG_INFO, "[WAPI-Debug]keystore_get's client_cert = %s", user_cert);
			wpa_printf(MSG_INFO, "[WAPI-Debug]keystore_get's ca_cert = %s", as_cert);

            /*get user's certificate*/
			if (os_strncmp("keystore://", (char *)user_cert, 11) == 0) {
                os_memset(value, 0, KEYSTORE_MESSAGE_SIZE);
                wpa_printf(MSG_INFO, "[WAPI-Debug]user_cert's key index = '%s' (len = %d)", &user_cert[11], os_strlen((char *)&user_cert[11]));
#ifdef WAPI_GET_CERT
                {
                    char user_cert_tmp[128];
                    int  key_len = os_strlen((char *)&user_cert[11]);

                    os_memcpy(user_cert_tmp, &user_cert[11], key_len);
                    user_cert_tmp[key_len]='\0';
                    length = wapi_keystore_get(wpa_s, user_cert_tmp, key_len, value);
                }
#else
                length = keystore_get((const u8)&user_cert[11],os_strlen((char *)&user_cert[11]), value);
#endif
                wpa_printf(MSG_INFO, "[WAPI-Debug]certificate lenght = %d", length);

                buffer_hexdump(MSG_DEBUG, "user_cert from supplicant=>",(const u8 *) value, 512);
				if (length == -1) {
						wpa_printf(MSG_ERROR, "[WAPI-Debug]: keystore_get fail_2-1!! get from keystore error!!\n");
						return -1;
				}

                if(length <= WAPI_CERTIFICATE_MAX_SIZE){
				    os_memcpy(&wapi_param.para.user[0], value, WAPI_CERTIFICATE_MAX_SIZE);
                }else {
                    wpa_printf(MSG_ERROR, "[WAPI-Debug]: keystore_get fail_2-2!! certificate too large!!\n");
						return -1;
                }
            }
			else {
				wpa_printf(MSG_ERROR, "[WAPI-Debug]: keystore_get fail_2-3!! the prefix is not keystore error\n");
				return -1;
			}

			/*Get as's certificate*/
			if (os_strncmp("keystore://", (char *)as_cert, 11) == 0) {
				os_memset(value, 0, KEYSTORE_MESSAGE_SIZE);
                wpa_printf(MSG_INFO, "[WAPI-Debug]ca_cert's key index = '%s' (len = %d)", &as_cert[11], os_strlen((char *)&as_cert[11]));

#ifdef WAPI_GET_CERT
                {
                    char as_cert_tmp[128];
                    int  key_len = os_strlen((char *)&as_cert[11]);
                    os_memcpy(as_cert_tmp, &as_cert[11], key_len);
                    as_cert_tmp[key_len]='\0';

                    length = wapi_keystore_get(wpa_s, as_cert_tmp, key_len, value);
                }
#else
                length = keystore_get((const u8)&as_cert[11],os_strlen((char *)&as_cert[11]), value);
#endif
                wpa_printf(MSG_INFO, "[WAPI-Debug]certificate lenght = %d", length);

                buffer_hexdump(MSG_DEBUG, "as_cert from suppliant=>",(const u8 *) value, 512);
				if (length == -1) {
						wpa_printf(MSG_ERROR, "[WAPI-Debug]: keystore_get fail_3-1!!get from keystore error!!\n");
						return -1;
				}

                if(length <= WAPI_CERTIFICATE_MAX_SIZE){
				    os_memcpy(&wapi_param.para.as[0], value, WAPI_CERTIFICATE_MAX_SIZE);
                }else {
                    wpa_printf(MSG_ERROR, "[WAPI-Debug]: keystore_get fail_3-2!! certificate too large!!\n");
						return -1;
                }
            }
			else {
				wpa_printf(MSG_ERROR, "[WAPI-Debug]: keystore_get fail_3-3!!the prefix is not keystore error\n");
				return -1;
			}

		}
		/*hard coding the user.cer and as.cer to wapi_param start ++*/
		#else
		{
			FILE* fp_user_cer = fopen((const char*)"/data/misc/wifi/user.cer", "rb");
			FILE* fp_user_as =  fopen((const char*)"/data/misc/wifi/as.cer", "rb");
			int len;

			if (fp_user_cer == NULL)
			{
				 wpa_printf(MSG_ERROR, "[WAPI-Debug]: fp_user_cer == NULL");
				return -1;
			}
			len = fread(&wapi_param.para.user[0], 1, 2048, fp_user_cer);
			fclose(fp_user_cer);
			if (len <= 0)
			{
				 wpa_printf(MSG_ERROR, "[WAPI-Debug]: len <= 0");
				return -1;
			}

			if (fp_user_as == NULL)
			{
				wpa_printf(MSG_ERROR, "[WAPI-Debug]: fp_user_cer == NULL");
				return -1;
			}
			len = fread(&wapi_param.para.as[0], 1, 2048, fp_user_as);
			fclose(fp_user_cer);
			if (len <= 0)
			{
				wpa_printf(MSG_ERROR, "[WAPI-Debug]: len <= 0");
				return -1;
			}
		}
		#endif
		/*hard coding the user.cer and as.cer to wapi_param start --*/

        wapi_set_param_result = wapi_set_user(&wapi_param);

        wpa_printf(MSG_WARNING, "WAPI: WAI_CNTAPPARA_SET result %d", wapi_set_param_result);
//MTK_OP01_PROTECT_START
#ifdef CONFIG_CMCC_SUPPORT /* CMCC */
        if (wapi_set_param_result < 0) {
            return -1;
        }
#endif
//MTK_OP01_PROTECT_END
    }
	return 0;
}


void wpa_supplicant_rx_wai(void *ctx, const u8 *src_addr, const u8 *buf, size_t len)
{
	wapi_set_rx_wai(buf, len);
}

#endif

/**
 * wpa_supplicant_set_suites - Set authentication and encryption parameters
 * @wpa_s: Pointer to wpa_supplicant data
 * @bss: Scan results for the selected BSS, or %NULL if not available
 * @ssid: Configuration data for the selected network
 * @wpa_ie: Buffer for the WPA/RSN IE
 * @wpa_ie_len: Maximum wpa_ie buffer size on input. This is changed to be the
 * used buffer length in case the functions returns success.
 * Returns: 0 on success or -1 on failure
 *
 * This function is used to configure authentication and encryption parameters
 * based on the network configuration and scan result for the selected BSS (if
 * available).
 */
int wpa_supplicant_set_suites(struct wpa_supplicant *wpa_s,
			      struct wpa_scan_res *bss,
			      struct wpa_ssid *ssid,
			      u8 *wpa_ie, size_t *wpa_ie_len)
{
	struct wpa_ie_data ie;
	int sel, proto;
	const u8 *bss_wpa, *bss_rsn;

#ifdef CONFIG_WAPI_SUPPORT
	/*if (bss && bss->wapi_ie_len && (ssid->proto & WPA_PROTO_WAPI) &&
		   wpa_parse_wapi_ie(bss->wapi_ie, bss->wapi_ie_len, &ie) == 0 &&
		   (ie.group_cipher & ssid->group_cipher) &&
		   (ie.pairwise_cipher & ssid->pairwise_cipher) &&
		   (ie.key_mgmt & ssid->key_mgmt)) {
		wpa_msg(wpa_s, MSG_DEBUG, "[WAPI] WPA: using WAPI");
		proto = WPA_PROTO_WAPI;
	}
	else*/ if (bss) {
		bss_wpa = wpa_scan_get_vendor_ie(bss, WPA_IE_VENDOR_TYPE);
		bss_rsn = wpa_scan_get_ie(bss, WLAN_EID_RSN);
	} else
		bss_wpa = bss_rsn = NULL;
#else
	if (bss) {
		bss_wpa = wpa_scan_get_vendor_ie(bss, WPA_IE_VENDOR_TYPE);
		bss_rsn = wpa_scan_get_ie(bss, WLAN_EID_RSN);
	} else
		bss_wpa = bss_rsn = NULL;
#endif
	if (bss_rsn && (ssid->proto & WPA_PROTO_RSN) &&
	    wpa_parse_wpa_ie(bss_rsn, 2 + bss_rsn[1], &ie) == 0 &&
	    (ie.group_cipher & ssid->group_cipher) &&
	    (ie.pairwise_cipher & ssid->pairwise_cipher) &&
	    (ie.key_mgmt & ssid->key_mgmt)) {
		wpa_msg(wpa_s, MSG_DEBUG, "RSN: using IEEE 802.11i/D9.0");
		proto = WPA_PROTO_RSN;
	} else if (bss_wpa && (ssid->proto & WPA_PROTO_WPA) &&
		   wpa_parse_wpa_ie(bss_wpa, 2 +bss_wpa[1], &ie) == 0 &&
		   (ie.group_cipher & ssid->group_cipher) &&
		   (ie.pairwise_cipher & ssid->pairwise_cipher) &&
		   (ie.key_mgmt & ssid->key_mgmt)) {
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using IEEE 802.11i/D3.0");
		proto = WPA_PROTO_WPA;
	} else if (bss) {
		wpa_msg(wpa_s, MSG_WARNING, "WPA: Failed to select WPA/RSN");
		return -1;
	} else {
		if (ssid->proto & WPA_PROTO_RSN)
			proto = WPA_PROTO_RSN;
#ifdef CONFIG_WAPI_SUPPORT
        else if (ssid->proto & WPA_PROTO_WAPI)
            proto = WPA_PROTO_WAPI;
#endif
		else
			proto = WPA_PROTO_WPA;
		if (wpa_supplicant_suites_from_ai(wpa_s, ssid, &ie) < 0) {
			os_memset(&ie, 0, sizeof(ie));
			ie.group_cipher = ssid->group_cipher;
			ie.pairwise_cipher = ssid->pairwise_cipher;
			ie.key_mgmt = ssid->key_mgmt;
#ifdef CONFIG_IEEE80211W
			ie.mgmt_group_cipher =
				ssid->ieee80211w != NO_IEEE80211W ?
				WPA_CIPHER_AES_128_CMAC : 0;
#endif /* CONFIG_IEEE80211W */
			wpa_printf(MSG_DEBUG, "WPA: Set cipher suites based "
				   "on configuration");
		} else
			proto = ie.proto;
	}

	wpa_printf(MSG_DEBUG, "WPA: Selected cipher suites: group %d "
		   "pairwise %d key_mgmt %d proto %d",
		   ie.group_cipher, ie.pairwise_cipher, ie.key_mgmt, proto);
#ifdef CONFIG_IEEE80211W
	if (ssid->ieee80211w) {
		wpa_printf(MSG_DEBUG, "WPA: Selected mgmt group cipher %d",
			   ie.mgmt_group_cipher);
	}
#endif /* CONFIG_IEEE80211W */

	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_PROTO, proto);
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_RSN_ENABLED,
			 !!(ssid->proto & WPA_PROTO_RSN));

	if (bss || !wpa_s->ap_ies_from_associnfo) {
		if (wpa_sm_set_ap_wpa_ie(wpa_s->wpa, bss_wpa,
					 bss_wpa ? 2 + bss_wpa[1] : 0) ||
		    wpa_sm_set_ap_rsn_ie(wpa_s->wpa, bss_rsn,
					 bss_rsn ? 2 + bss_rsn[1] : 0))
			return -1;
	}

	sel = ie.group_cipher & ssid->group_cipher;
	if (sel & WPA_CIPHER_CCMP) {
		wpa_s->group_cipher = WPA_CIPHER_CCMP;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using GTK CCMP");
	} else if (sel & WPA_CIPHER_TKIP) {
		wpa_s->group_cipher = WPA_CIPHER_TKIP;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using GTK TKIP");
	} else if (sel & WPA_CIPHER_WEP104) {
		wpa_s->group_cipher = WPA_CIPHER_WEP104;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using GTK WEP104");
	} else if (sel & WPA_CIPHER_WEP40) {
		wpa_s->group_cipher = WPA_CIPHER_WEP40;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using GTK WEP40");
	}
#ifdef CONFIG_WAPI_SUPPORT
    else if (sel & WAPI_CIPHER_SMS4) {
        wpa_s->group_cipher = WAPI_CIPHER_SMS4;
        wpa_msg(wpa_s, MSG_DEBUG, "[WAPI]WPA: using SMS4");
    }
#endif
	else {
		wpa_printf(MSG_WARNING, "WPA: Failed to select group cipher.");
		return -1;
	}

	sel = ie.pairwise_cipher & ssid->pairwise_cipher;
	if (sel & WPA_CIPHER_CCMP) {
		wpa_s->pairwise_cipher = WPA_CIPHER_CCMP;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using PTK CCMP");
	} else if (sel & WPA_CIPHER_TKIP) {
		wpa_s->pairwise_cipher = WPA_CIPHER_TKIP;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using PTK TKIP");
	} else if (sel & WPA_CIPHER_NONE) {
		wpa_s->pairwise_cipher = WPA_CIPHER_NONE;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using PTK NONE");
	}
#ifdef CONFIG_WAPI_SUPPORT
    else if (sel & WAPI_CIPHER_SMS4) {
        wpa_s->pairwise_cipher = WAPI_CIPHER_SMS4;
        wpa_msg(wpa_s, MSG_DEBUG, "[WAPI]WPA: using SMS4");
    }
#endif
	else {
		wpa_printf(MSG_WARNING, "WPA: Failed to select pairwise "
			   "cipher.");
		return -1;
	}

	sel = ie.key_mgmt & ssid->key_mgmt;
	if (0) {
#ifdef CONFIG_IEEE80211R
	} else if (sel & WPA_KEY_MGMT_FT_IEEE8021X) {
		wpa_s->key_mgmt = WPA_KEY_MGMT_FT_IEEE8021X;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using KEY_MGMT FT/802.1X");
	} else if (sel & WPA_KEY_MGMT_FT_PSK) {
		wpa_s->key_mgmt = WPA_KEY_MGMT_FT_PSK;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using KEY_MGMT FT/PSK");
#endif /* CONFIG_IEEE80211R */
#ifdef CONFIG_IEEE80211W
	} else if (sel & WPA_KEY_MGMT_IEEE8021X_SHA256) {
		wpa_s->key_mgmt = WPA_KEY_MGMT_IEEE8021X_SHA256;
		wpa_msg(wpa_s, MSG_DEBUG,
			"WPA: using KEY_MGMT 802.1X with SHA256");
	} else if (sel & WPA_KEY_MGMT_PSK_SHA256) {
		wpa_s->key_mgmt = WPA_KEY_MGMT_PSK_SHA256;
		wpa_msg(wpa_s, MSG_DEBUG,
			"WPA: using KEY_MGMT PSK with SHA256");
#endif /* CONFIG_IEEE80211W */
	} else if (sel & WPA_KEY_MGMT_IEEE8021X) {
		wpa_s->key_mgmt = WPA_KEY_MGMT_IEEE8021X;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using KEY_MGMT 802.1X");
	} else if (sel & WPA_KEY_MGMT_PSK) {
		wpa_s->key_mgmt = WPA_KEY_MGMT_PSK;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using KEY_MGMT WPA-PSK");
	} else if (sel & WPA_KEY_MGMT_WPA_NONE) {
		wpa_s->key_mgmt = WPA_KEY_MGMT_WPA_NONE;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using KEY_MGMT WPA-NONE");
	}
#ifdef CONFIG_WAPI_SUPPORT
    else if (sel & WAPI_KEY_MGMT_CERT) {
        wpa_s->key_mgmt = WAPI_KEY_MGMT_CERT;
        wpa_s->proto = WPA_PROTO_WAPI;
        wpa_msg(wpa_s, MSG_DEBUG, "WPA: using KEY_MGMT WAPI WAI");
    }  else if (sel & WAPI_KEY_MGMT_PSK) {
        wpa_s->key_mgmt = WAPI_KEY_MGMT_PSK;
        wpa_s->proto = WPA_PROTO_WAPI;
        wpa_msg(wpa_s, MSG_DEBUG, "WPA: using KEY_MGMT WAPI PSK");
    }
#endif
	else {
		wpa_printf(MSG_WARNING, "WPA: Failed to select authenticated "
			   "key management type.");
		return -1;
	}

#ifdef CONFIG_WAPI_SUPPORT
	if(proto != WPA_PROTO_WAPI)
	{
		wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_KEY_MGMT, wpa_s->key_mgmt);
		wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_PAIRWISE,
				 wpa_s->pairwise_cipher);
		wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_GROUP, wpa_s->group_cipher);

#ifdef CONFIG_IEEE80211W
		sel = ie.mgmt_group_cipher;
		if (ssid->ieee80211w == NO_IEEE80211W ||
		    !(ie.capabilities & WPA_CAPABILITY_MGMT_FRAME_PROTECTION))
			sel = 0;
		if (sel & WPA_CIPHER_AES_128_CMAC) {
			wpa_s->mgmt_group_cipher = WPA_CIPHER_AES_128_CMAC;
			wpa_msg(wpa_s, MSG_DEBUG, "WPA: using MGMT group cipher "
				"AES-128-CMAC");
		} else {
			wpa_s->mgmt_group_cipher = 0;
			wpa_msg(wpa_s, MSG_DEBUG, "WPA: not using MGMT group cipher");
		}
		wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_MGMT_GROUP,
				 wpa_s->mgmt_group_cipher);
#endif /* CONFIG_IEEE80211W */

		if (wpa_sm_set_assoc_wpa_ie_default(wpa_s->wpa, wpa_ie, wpa_ie_len)) {
			wpa_printf(MSG_WARNING, "WPA: Failed to generate WPA IE.");
			return -1;
		}

		if (ssid->key_mgmt & WPA_KEY_MGMT_PSK)
			wpa_sm_set_pmk(wpa_s->wpa, ssid->psk, PMK_LEN);
		else
			wpa_sm_set_pmk_from_pmksa(wpa_s->wpa);
	}
	else {
		//
		//fill wapi ie for associate request
		//
		*wpa_ie_len = wpa_gen_wapi_ie(wpa_s, wpa_ie);
	}
#else
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_KEY_MGMT, wpa_s->key_mgmt);
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_PAIRWISE,
			 wpa_s->pairwise_cipher);
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_GROUP, wpa_s->group_cipher);

#ifdef CONFIG_IEEE80211W
	sel = ie.mgmt_group_cipher;
	if (ssid->ieee80211w == NO_IEEE80211W ||
	    !(ie.capabilities & WPA_CAPABILITY_MFPC))
		sel = 0;
	if (sel & WPA_CIPHER_AES_128_CMAC) {
		wpa_s->mgmt_group_cipher = WPA_CIPHER_AES_128_CMAC;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: using MGMT group cipher "
			"AES-128-CMAC");
	} else {
		wpa_s->mgmt_group_cipher = 0;
		wpa_msg(wpa_s, MSG_DEBUG, "WPA: not using MGMT group cipher");
	}
	wpa_sm_set_param(wpa_s->wpa, WPA_PARAM_MGMT_GROUP,
			 wpa_s->mgmt_group_cipher);
#endif /* CONFIG_IEEE80211W */

	if (wpa_sm_set_assoc_wpa_ie_default(wpa_s->wpa, wpa_ie, wpa_ie_len)) {
		wpa_printf(MSG_WARNING, "WPA: Failed to generate WPA IE.");
		return -1;
	}

	if (ssid->key_mgmt &
	    (WPA_KEY_MGMT_PSK | WPA_KEY_MGMT_FT_PSK | WPA_KEY_MGMT_PSK_SHA256))
		wpa_sm_set_pmk(wpa_s->wpa, ssid->psk, PMK_LEN);
	else
		wpa_sm_set_pmk_from_pmksa(wpa_s->wpa);
#endif

	return 0;
}


/**
 * wpa_supplicant_associate - Request association
 * @wpa_s: Pointer to wpa_supplicant data
 * @bss: Scan results for the selected BSS, or %NULL if not available
 * @ssid: Configuration data for the selected network
 *
 * This function is used to request %wpa_supplicant to associate with a BSS.
 */
void wpa_supplicant_associate(struct wpa_supplicant *wpa_s,
			      struct wpa_scan_res *bss, struct wpa_ssid *ssid)
{
	u8 wpa_ie[80];
	size_t wpa_ie_len;
	int use_crypt, ret, i;
	int algs = AUTH_ALG_OPEN_SYSTEM;
	wpa_cipher cipher_pairwise, cipher_group;
	struct wpa_driver_associate_params params;
	int wep_keys_set = 0;
	struct wpa_driver_capa capa;
	int assoc_failed = 0;

	wpa_s->reassociate = 0;
	if (bss) {
#ifdef CONFIG_IEEE80211R
		const u8 *md = NULL;
#endif /* CONFIG_IEEE80211R */
		const u8 *ie = wpa_scan_get_ie(bss, WLAN_EID_SSID);
		wpa_msg(wpa_s, MSG_INFO, "Trying to associate with " MACSTR
			" (SSID='%s' freq=%d MHz)", MAC2STR(bss->bssid),
			ie ? wpa_ssid_txt(ie + 2, ie[1]) : "", bss->freq);
		os_memset(wpa_s->bssid, 0, ETH_ALEN);
		os_memcpy(wpa_s->pending_bssid, bss->bssid, ETH_ALEN);
		wpa_s->link_speed = wpa_scan_get_max_rate(bss) * 500000;
		wpa_s->rssi = bss->level;
#ifdef CONFIG_IEEE80211R
		ie = wpa_scan_get_ie(bss, WLAN_EID_MOBILITY_DOMAIN);
		if (ie && ie[1] >= MOBILITY_DOMAIN_ID_LEN)
			md = ie + 2;
		wpa_sm_set_ft_params(wpa_s->wpa, md, NULL, 0, NULL);
		if (md) {
			/* Prepare for the next transition */
			wpa_ft_prepare_auth_request(wpa_s->wpa);
		}
#endif /* CONFIG_IEEE80211R */
#ifdef CONFIG_WPS
	} else if ((ssid->ssid == NULL || ssid->ssid_len == 0) &&
		   wpa_s->conf->ap_scan == 2 &&
		   (ssid->key_mgmt & WPA_KEY_MGMT_WPS)) {
		/* Use ap_scan==1 style network selection to find the network
		 */
		wpa_s->scan_req = 2;
		wpa_s->reassociate = 1;
		wpa_supplicant_req_scan(wpa_s, 0, 0);
		return;
#endif /* CONFIG_WPS */
	} else {
		wpa_msg(wpa_s, MSG_INFO, "Trying to associate with SSID '%s'",
			wpa_ssid_txt(ssid->ssid, ssid->ssid_len));
		os_memset(wpa_s->pending_bssid, 0, ETH_ALEN);
	}
	wpa_supplicant_cancel_scan(wpa_s);

	/* Starting new association, so clear the possibly used WPA IE from the
	 * previous association. */
	wpa_sm_set_assoc_wpa_ie(wpa_s->wpa, NULL, 0);

	if (wpa_drv_set_mode(wpa_s, ssid->mode)) {
		wpa_printf(MSG_WARNING, "Failed to set operating mode");
		assoc_failed = 1;
	}

#ifdef IEEE8021X_EAPOL
	if (ssid->key_mgmt & WPA_KEY_MGMT_IEEE8021X_NO_WPA) {
		if (ssid->leap) {
			if (ssid->non_leap == 0)
				algs = AUTH_ALG_LEAP;
			else
				algs |= AUTH_ALG_LEAP;
		}
	}
#endif /* IEEE8021X_EAPOL */
	wpa_printf(MSG_DEBUG, "Automatic auth_alg selection: 0x%x", algs);
	if (ssid->auth_alg) {
		algs = 0;
		if (ssid->auth_alg & WPA_AUTH_ALG_OPEN)
			algs |= AUTH_ALG_OPEN_SYSTEM;
		if (ssid->auth_alg & WPA_AUTH_ALG_SHARED)
			algs |= AUTH_ALG_SHARED_KEY;
		if (ssid->auth_alg & WPA_AUTH_ALG_LEAP)
			algs |= AUTH_ALG_LEAP;
		wpa_printf(MSG_DEBUG, "Overriding auth_alg selection: 0x%x",
			   algs);
	}
	wpa_drv_set_auth_alg(wpa_s, algs);

	if (bss && (wpa_scan_get_vendor_ie(bss, WPA_IE_VENDOR_TYPE) ||
		    wpa_scan_get_ie(bss, WLAN_EID_RSN)) &&
	    (ssid->key_mgmt & (WPA_KEY_MGMT_IEEE8021X | WPA_KEY_MGMT_PSK |
			       WPA_KEY_MGMT_FT_IEEE8021X |
			       WPA_KEY_MGMT_FT_PSK |
			       WPA_KEY_MGMT_IEEE8021X_SHA256 |
			       WPA_KEY_MGMT_PSK_SHA256))) {
		int try_opportunistic;
		try_opportunistic = ssid->proactive_key_caching &&
			(ssid->proto & WPA_PROTO_RSN);
		if (pmksa_cache_set_current(wpa_s->wpa, NULL, bss->bssid,
					    wpa_s->current_ssid,
					    try_opportunistic) == 0)
			eapol_sm_notify_pmkid_attempt(wpa_s->eapol, 1);
		wpa_ie_len = sizeof(wpa_ie);
		if (wpa_supplicant_set_suites(wpa_s, bss, ssid,
					      wpa_ie, &wpa_ie_len)) {
			wpa_printf(MSG_WARNING, "WPA: Failed to set WPA key "
				   "management and encryption suites");
			return;
		}
	} else if (ssid->key_mgmt &
		   (WPA_KEY_MGMT_PSK | WPA_KEY_MGMT_IEEE8021X |
		    WPA_KEY_MGMT_WPA_NONE | WPA_KEY_MGMT_FT_PSK |
		    WPA_KEY_MGMT_FT_IEEE8021X | WPA_KEY_MGMT_PSK_SHA256 |
		    WPA_KEY_MGMT_IEEE8021X_SHA256)) {
		wpa_ie_len = sizeof(wpa_ie);
		if (wpa_supplicant_set_suites(wpa_s, NULL, ssid,
					      wpa_ie, &wpa_ie_len)) {
			wpa_printf(MSG_WARNING, "WPA: Failed to set WPA key "
				   "management and encryption suites (no scan "
				   "results)");
			return;
		}
#ifdef CONFIG_WPS
	}
	else if (ssid->key_mgmt & WPA_KEY_MGMT_WPS) {
		struct wpabuf *wps_ie;
		wps_ie = wps_build_assoc_req_ie(wpas_wps_get_req_type(ssid));
		if (wps_ie && wpabuf_len(wps_ie) <= sizeof(wpa_ie)) {
			wpa_ie_len = wpabuf_len(wps_ie);
			os_memcpy(wpa_ie, wpabuf_head(wps_ie), wpa_ie_len);
		} else
			wpa_ie_len = 0;
		wpabuf_free(wps_ie);
		wpa_supplicant_set_non_wpa_policy(wpa_s, ssid);
#endif /* CONFIG_WPS */
	}
#ifdef CONFIG_WAPI_SUPPORT
    else if (ssid->key_mgmt & (WAPI_KEY_MGMT_PSK | WAPI_KEY_MGMT_CERT))
    {
    	wpa_printf(MSG_DEBUG, "[WAPI-Debug] wpa_supplicant_set_suites");
        if (wpa_supplicant_set_suites(wpa_s, NULL, ssid,
            wpa_ie, &wpa_ie_len))
        {
            wpa_printf(MSG_WARNING, "WPA: Failed to set WPA key "
                "management and encryption suites (no scan "
                "results)");
            return;
        }
    }
#endif
	else {
		wpa_supplicant_set_non_wpa_policy(wpa_s, ssid);
		wpa_ie_len = 0;
	}

	wpa_clear_keys(wpa_s, bss ? bss->bssid : NULL);
	use_crypt = 1;
	cipher_pairwise = cipher_suite2driver(wpa_s->pairwise_cipher);
	cipher_group = cipher_suite2driver(wpa_s->group_cipher);
	if (wpa_s->key_mgmt == WPA_KEY_MGMT_NONE ||
	    wpa_s->key_mgmt == WPA_KEY_MGMT_IEEE8021X_NO_WPA) {
		if (wpa_s->key_mgmt == WPA_KEY_MGMT_NONE)
			use_crypt = 0;
		if (wpa_set_wep_keys(wpa_s, ssid)) {
			use_crypt = 1;
			wep_keys_set = 1;
		}
	}
	if (wpa_s->key_mgmt == WPA_KEY_MGMT_WPS)
		use_crypt = 0;

#ifdef IEEE8021X_EAPOL
	if (wpa_s->key_mgmt == WPA_KEY_MGMT_IEEE8021X_NO_WPA) {
		if ((ssid->eapol_flags &
		     (EAPOL_FLAG_REQUIRE_KEY_UNICAST |
		      EAPOL_FLAG_REQUIRE_KEY_BROADCAST)) == 0 &&
		    !wep_keys_set) {
			use_crypt = 0;
		} else {
			/* Assume that dynamic WEP-104 keys will be used and
			 * set cipher suites in order for drivers to expect
			 * encryption. */
			cipher_pairwise = cipher_group = CIPHER_WEP104;
		}
	}
#endif /* IEEE8021X_EAPOL */

	if (wpa_s->key_mgmt == WPA_KEY_MGMT_WPA_NONE) {
		/* Set the key before (and later after) association */
		wpa_supplicant_set_wpa_none_key(wpa_s, ssid);
	}

	wpa_drv_set_drop_unencrypted(wpa_s, use_crypt);
	wpa_supplicant_set_state(wpa_s, WPA_ASSOCIATING);
	os_memset(&params, 0, sizeof(params));
	if (bss) {
		const u8 *ie = wpa_scan_get_ie(bss, WLAN_EID_SSID);
		params.bssid = bss->bssid;
		params.ssid = ie ? ie + 2 : (u8 *) "";
		params.ssid_len = ie ? ie[1] : 0;
		params.freq = bss->freq;
	} else {
		params.ssid = ssid->ssid;
		params.ssid_len = ssid->ssid_len;
	}
	if (ssid->mode == 1 && ssid->frequency > 0 && params.freq == 0)
		params.freq = ssid->frequency; /* Initial channel for IBSS */
	params.wpa_ie = wpa_ie;
	params.wpa_ie_len = wpa_ie_len;
	params.pairwise_suite = cipher_pairwise;
	params.group_suite = cipher_group;
	params.key_mgmt_suite = key_mgmt2driver(wpa_s->key_mgmt);
	params.auth_alg = algs;
	params.mode = ssid->mode;

	wpa_printf(MSG_DEBUG, "[WAPI-Debug] %s  cipher_paiwise = %d, cipher_group = %d, key_mgmt_suite = %d", __FUNCTION__,
		cipher_pairwise, cipher_group, key_mgmt2driver(wpa_s->key_mgmt));


	for (i = 0; i < NUM_WEP_KEYS; i++) {
		if (ssid->wep_key_len[i])
			params.wep_key[i] = ssid->wep_key[i];
		params.wep_key_len[i] = ssid->wep_key_len[i];
	}
	params.wep_tx_keyidx = ssid->wep_tx_keyidx;

	if (wpa_s->driver_4way_handshake &&
	    (params.key_mgmt_suite == KEY_MGMT_PSK ||
	     params.key_mgmt_suite == KEY_MGMT_FT_PSK)) {
		params.passphrase = ssid->passphrase;
		if (ssid->psk_set)
			params.psk = ssid->psk;
	}

#ifdef CONFIG_IEEE80211W
	switch (ssid->ieee80211w) {
	case NO_IEEE80211W:
		params.mgmt_frame_protection = NO_MGMT_FRAME_PROTECTION;
		break;
	case IEEE80211W_OPTIONAL:
		params.mgmt_frame_protection = MGMT_FRAME_PROTECTION_OPTIONAL;
		break;
	case IEEE80211W_REQUIRED:
		params.mgmt_frame_protection = MGMT_FRAME_PROTECTION_REQUIRED;
		break;
	}
	if (ssid->ieee80211w != NO_IEEE80211W && bss) {
		const u8 *rsn = wpa_scan_get_ie(bss, WLAN_EID_RSN);
		struct wpa_ie_data ie;
		if (rsn && wpa_parse_wpa_ie(rsn, 2 + rsn[1], &ie) == 0 &&
		    ie.capabilities &
		    (WPA_CAPABILITY_MFPC | WPA_CAPABILITY_MFPR)) {
			wpa_printf(MSG_DEBUG, "WPA: Selected AP supports MFP: "
				   "require MFP");
			params.mgmt_frame_protection =
				MGMT_FRAME_PROTECTION_REQUIRED;
		}
	}
#endif /* CONFIG_IEEE80211W */

#ifdef CONFIG_WAPI_SUPPORT
	if((wpa_s->key_mgmt == WAPI_KEY_MGMT_PSK)||(wpa_s->key_mgmt == WAPI_KEY_MGMT_CERT)){
		if(wpa_supplincant_set_wapi_param(wpa_s,ssid) < 0){
			wpa_msg(wpa_s, MSG_INFO, "[WAPI] wpa_supplincant_set_wapi_param "
				"failed");
//MTK_OP01_PROTECT_START
#ifdef CONFIG_CMCC_SUPPORT /* CMCC */
			wpa_msg(wpa_s, MSG_INFO, WPA_EVENT_EAP_NO_CERTIFICATION);
#endif
//MTK_OP01_PROTECT_END
            wpa_printf(MSG_ERROR, "[WAPI] Set wapi param fail "
                ": we don't associate with the profile\n"
                "The casue is certificate invalid or password field lacked\n");
            return;
		}
	}
#endif

	if (wpa_s->use_client_mlme)
		ret = ieee80211_sta_associate(wpa_s, &params);
	else
		ret = wpa_drv_associate(wpa_s, &params);
	if (ret < 0) {
		wpa_msg(wpa_s, MSG_INFO, "Association request to the driver "
			"failed");
		/* try to continue anyway; new association will be tried again
		 * after timeout */
		assoc_failed = 1;
	}

	if (wpa_s->key_mgmt == WPA_KEY_MGMT_WPA_NONE) {
		/* Set the key after the association just in case association
		 * cleared the previously configured key. */
		wpa_supplicant_set_wpa_none_key(wpa_s, ssid);
		/* No need to timeout authentication since there is no key
		 * management. */
		wpa_supplicant_cancel_auth_timeout(wpa_s);
		wpa_supplicant_set_state(wpa_s, WPA_COMPLETED);
	} else {
		/* Timeout for IEEE 802.11 authentication and association */
		int timeout = 60;

		if (assoc_failed) {
			/* give IBSS a bit more time */
 			timeout = ssid->mode ? 10 : 5;
		} else if (wpa_s->conf->ap_scan == 1) {
			/* give IBSS a bit more time */
 			timeout = ssid->mode ? 20 : 10;
		}
#ifdef CONFIG_WAPI_SUPPORT
    if((wpa_s->key_mgmt & WAPI_KEY_MGMT_CERT) !=0 || (wpa_s->key_mgmt & WAPI_KEY_MGMT_PSK) !=0){
        wpa_printf(MSG_DEBUG, "[WAPI-Debug] set WAPI Auth Time in 35 secs\n");
        wpa_supplicant_req_auth_timeout(wpa_s, 35, 0);
    }
    else {
		wpa_supplicant_req_auth_timeout(wpa_s, timeout, 0);
	}
#else
		wpa_supplicant_req_auth_timeout(wpa_s, timeout, 0);
#endif

    }

#ifdef CONFIG_WAPI_SUPPORT
	if((wpa_s->key_mgmt & WAPI_KEY_MGMT_CERT) !=0 || (wpa_s->key_mgmt & WAPI_KEY_MGMT_PSK) !=0)
	{
		wpa_s->current_ssid = ssid;
		wpa_printf(MSG_DEBUG, "[WAPI-Debug] not to initiate eapol and others\n");
		return;
	}
#endif

	if (wep_keys_set && wpa_drv_get_capa(wpa_s, &capa) == 0 &&
	    capa.flags & WPA_DRIVER_FLAGS_SET_KEYS_AFTER_ASSOC) {
		/* Set static WEP keys again */
		wpa_set_wep_keys(wpa_s, ssid);
	}

	if (wpa_s->current_ssid && wpa_s->current_ssid != ssid) {
		/*
		 * Do not allow EAP session resumption between different
		 * network configurations.
		 */
		eapol_sm_invalidate_cached_session(wpa_s->eapol);
	}
	wpa_s->current_ssid = ssid;
	wpa_supplicant_rsn_supp_set_config(wpa_s, wpa_s->current_ssid);
	wpa_supplicant_initiate_eapol(wpa_s);
}


/**
 * wpa_supplicant_disassociate - Disassociate the current connection
 * @wpa_s: Pointer to wpa_supplicant data
 * @reason_code: IEEE 802.11 reason code for the disassociate frame
 *
 * This function is used to request %wpa_supplicant to disassociate with the
 * current AP.
 */
void wpa_supplicant_disassociate(struct wpa_supplicant *wpa_s,
				 int reason_code)
{
	u8 *addr = NULL;
	if (!is_zero_ether_addr(wpa_s->bssid)) {
		if (wpa_s->use_client_mlme)
			ieee80211_sta_disassociate(wpa_s, reason_code);
		else
			wpa_drv_disassociate(wpa_s, wpa_s->bssid, reason_code);
		addr = wpa_s->bssid;
	}
	wpa_clear_keys(wpa_s, addr);
	wpa_supplicant_mark_disassoc(wpa_s);
	wpa_s->current_ssid = NULL;
	wpa_sm_set_config(wpa_s->wpa, NULL);
	eapol_sm_notify_config(wpa_s->eapol, NULL, NULL);
}


/**
 * wpa_supplicant_deauthenticate - Deauthenticate the current connection
 * @wpa_s: Pointer to wpa_supplicant data
 * @reason_code: IEEE 802.11 reason code for the deauthenticate frame
 *
 * This function is used to request %wpa_supplicant to deauthenticate from the
 * current AP.
 */
void wpa_supplicant_deauthenticate(struct wpa_supplicant *wpa_s,
				   int reason_code)
{
	u8 *addr = NULL;
	if (!is_zero_ether_addr(wpa_s->bssid)) {
		if (wpa_s->use_client_mlme)
			ieee80211_sta_deauthenticate(wpa_s, reason_code);
		else
			wpa_drv_deauthenticate(wpa_s, wpa_s->bssid,
					       reason_code);
		addr = wpa_s->bssid;
	}
	wpa_clear_keys(wpa_s, addr);
	wpa_supplicant_mark_disassoc(wpa_s);
	wpa_s->current_ssid = NULL;
	wpa_sm_set_config(wpa_s->wpa, NULL);
	eapol_sm_notify_config(wpa_s->eapol, NULL, NULL);
}


static int wpa_supplicant_get_scan_results_old(struct wpa_supplicant *wpa_s)
{
#define SCAN_AP_LIMIT 128
	struct wpa_scan_result *results;
	int num, i;
	struct wpa_scan_results *res;

	results = os_malloc(SCAN_AP_LIMIT * sizeof(struct wpa_scan_result));
	if (results == NULL) {
		wpa_printf(MSG_WARNING, "Failed to allocate memory for scan "
			   "results");
		return -1;
	}

	num = wpa_drv_get_scan_results(wpa_s, results, SCAN_AP_LIMIT);
	wpa_printf(MSG_DEBUG, "Scan results: %d", num);
	if (num < 0) {
		wpa_printf(MSG_DEBUG, "Failed to get scan results");
		os_free(results);
		return -1;
	}
	if (num > SCAN_AP_LIMIT) {
		wpa_printf(MSG_INFO, "Not enough room for all APs (%d < %d)",
			   num, SCAN_AP_LIMIT);
		num = SCAN_AP_LIMIT;
	}

	wpa_scan_results_free(wpa_s->scan_res);
	wpa_s->scan_res = NULL;

	/* Convert old scan result data structure to the new one */
	res = os_zalloc(sizeof(*res));
	if (res == NULL) {
		os_free(results);
		return -1;
	}
	res->res = os_zalloc(num * sizeof(struct wpa_scan_res *));
	if (res->res == NULL) {
		os_free(results);
		os_free(res);
		return -1;
	}

	for (i = 0; i < num; i++) {
		struct wpa_scan_result *bss = &results[i];
		struct wpa_scan_res *r;
		size_t ie_len;
		u8 *pos;

		ie_len = 2 + bss->ssid_len + bss->rsn_ie_len + bss->wpa_ie_len;
		if (bss->maxrate)
			ie_len += 3;
		if (bss->mdie_present)
			ie_len += 5;

		r = os_zalloc(sizeof(*r) + ie_len);
		if (r == NULL)
			break;

		os_memcpy(r->bssid, bss->bssid, ETH_ALEN);
		r->freq = bss->freq;
		r->caps = bss->caps;
		r->qual = bss->qual;
		r->noise = bss->noise;
		r->level = bss->level;
		r->tsf = bss->tsf;
		r->ie_len = ie_len;

		pos = (u8 *) (r + 1);

		/* SSID IE */
		*pos++ = WLAN_EID_SSID;
		*pos++ = bss->ssid_len;
		os_memcpy(pos, bss->ssid, bss->ssid_len);
		pos += bss->ssid_len;

		if (bss->maxrate) {
			/* Fake Supported Rate IE to include max rate */
			*pos++ = WLAN_EID_SUPP_RATES;
			*pos++ = 1;
			*pos++ = bss->maxrate;
		}

		if (bss->rsn_ie_len) {
			os_memcpy(pos, bss->rsn_ie, bss->rsn_ie_len);
			pos += bss->rsn_ie_len;
		}

		if (bss->mdie_present) {
			os_memcpy(pos, bss->mdie, 5);
			pos += 5;
		}

		if (bss->wpa_ie_len) {
			os_memcpy(pos, bss->wpa_ie, bss->wpa_ie_len);
			pos += bss->wpa_ie_len;
		}

		res->res[res->num++] = r;
	}

	os_free(results);
	wpa_s->scan_res = res;

	return 0;
}


/**
 * wpa_supplicant_get_scan_results - Get scan results
 * @wpa_s: Pointer to wpa_supplicant data
 * Returns: 0 on success, -1 on failure
 *
 * This function is request the current scan results from the driver and stores
 * a local copy of the results in wpa_s->scan_res.
 */
int wpa_supplicant_get_scan_results(struct wpa_supplicant *wpa_s)
{
	int ret;

	if (wpa_s->use_client_mlme) {
		wpa_scan_results_free(wpa_s->scan_res);
		wpa_s->scan_res = ieee80211_sta_get_scan_results(wpa_s);
		if (wpa_s->scan_res == NULL) {
			wpa_printf(MSG_DEBUG, "Failed to get scan results");
			ret = -1;
		} else
			ret = 0;
	} else if (wpa_s->driver->get_scan_results2 == NULL)
		ret = wpa_supplicant_get_scan_results_old(wpa_s);
	else {
		wpa_scan_results_free(wpa_s->scan_res);
		wpa_s->scan_res = wpa_drv_get_scan_results2(wpa_s);
		if (wpa_s->scan_res == NULL) {
			wpa_printf(MSG_DEBUG, "Failed to get scan results");
			ret = -1;
		} else
			ret = 0;
	}

	if (wpa_s->scan_res)
		wpa_scan_sort_results(wpa_s->scan_res);

	return ret;
}


/**
 * wpa_supplicant_get_ssid - Get a pointer to the current network structure
 * @wpa_s: Pointer to wpa_supplicant data
 * Returns: A pointer to the current network structure or %NULL on failure
 */
struct wpa_ssid * wpa_supplicant_get_ssid(struct wpa_supplicant *wpa_s)
{
	struct wpa_ssid *entry;
	u8 ssid[MAX_SSID_LEN];
	int res;
	size_t ssid_len;
	u8 bssid[ETH_ALEN];
	int wired;

	if (wpa_s->use_client_mlme) {
		if (ieee80211_sta_get_ssid(wpa_s, ssid, &ssid_len)) {
			wpa_printf(MSG_WARNING, "Could not read SSID from "
				   "MLME.");
			return NULL;
		}
	} else {
		res = wpa_drv_get_ssid(wpa_s, ssid);
		if (res < 0) {
			wpa_printf(MSG_WARNING, "Could not read SSID from "
				   "driver.");
			return NULL;
		}
		ssid_len = res;
	}

	if (wpa_s->use_client_mlme)
		os_memcpy(bssid, wpa_s->bssid, ETH_ALEN);
	else if (wpa_drv_get_bssid(wpa_s, bssid) < 0) {
		wpa_printf(MSG_WARNING, "Could not read BSSID from driver.");
		return NULL;
	}

	wired = wpa_s->conf->ap_scan == 0 && wpa_s->driver &&
		IS_WIRED(wpa_s->driver);

	entry = wpa_s->conf->ssid;
	while (entry) {
		if (!entry->disabled &&
		    ((ssid_len == entry->ssid_len &&
		      os_memcmp(ssid, entry->ssid, ssid_len) == 0) || wired) &&
		    (!entry->bssid_set ||
		     os_memcmp(bssid, entry->bssid, ETH_ALEN) == 0))
			return entry;
#ifdef CONFIG_WPS
		if (!entry->disabled &&
		    (entry->key_mgmt & WPA_KEY_MGMT_WPS) &&
		    (entry->ssid == NULL || entry->ssid_len == 0) &&
		    (!entry->bssid_set ||
		     os_memcmp(bssid, entry->bssid, ETH_ALEN) == 0))
			return entry;
#endif /* CONFIG_WPS */
		entry = entry->next;
	}

	return NULL;
}

int wpa_drv_scan(struct wpa_supplicant *wpa_s, struct wpa_ssid **ssid_ptr)
{
	u8 *ssid_nm = NULL;
	size_t ssid_len = 0;
	int ret = -1;

	if (wpa_s->driver->combo_scan) {
		ret = wpa_s->driver->combo_scan(wpa_s->drv_priv, ssid_ptr,
						wpa_s->conf->ssid);
		wpa_s->prev_scan_ssid = (*ssid_ptr && !ret) ?
				(*ssid_ptr) : BROADCAST_SSID_SCAN;
	}
	else if (wpa_s->driver->scan) {
		if (*ssid_ptr) {
			ssid_nm = (*ssid_ptr)->ssid;
			ssid_len = (*ssid_ptr)->ssid_len;
		}

		ret = wpa_s->driver->scan(wpa_s->drv_priv, ssid_nm, ssid_len);
	}
	return ret;
}

static int wpa_supplicant_set_driver(struct wpa_supplicant *wpa_s,
				     const char *name)
{
	int i;

	if (wpa_s == NULL)
		return -1;

	if (wpa_supplicant_drivers[0] == NULL) {
		wpa_printf(MSG_ERROR, "No driver interfaces build into "
			   "wpa_supplicant.");
		return -1;
	}

	if (name == NULL) {
		/* default to first driver in the list */
		wpa_s->driver = wpa_supplicant_drivers[0];
		wpa_s->global_drv_priv = wpa_s->global->drv_priv[0];
		return 0;
	}

	for (i = 0; wpa_supplicant_drivers[i]; i++) {
		if (os_strcmp(name, wpa_supplicant_drivers[i]->name) == 0) {
			wpa_s->driver = wpa_supplicant_drivers[i];
			wpa_s->global_drv_priv = wpa_s->global->drv_priv[i];
			return 0;
		}
	}

	wpa_printf(MSG_ERROR, "Unsupported driver '%s'.\n", name);
	return -1;
}


void wpa_supplicant_rx_eapol(void *ctx, const u8 *src_addr,
			     const u8 *buf, size_t len)
{
	struct wpa_supplicant *wpa_s = ctx;

	wpa_printf(MSG_DEBUG, "RX EAPOL from " MACSTR, MAC2STR(src_addr));
	wpa_hexdump(MSG_MSGDUMP, "RX EAPOL", buf, len);

	if (wpa_s->wpa_state < WPA_ASSOCIATED) {
		/*
		 * There is possible race condition between receiving the
		 * association event and the EAPOL frame since they are coming
		 * through different paths from the driver. In order to avoid
		 * issues in trying to process the EAPOL frame before receiving
		 * association information, lets queue it for processing until
		 * the association event is received.
		 */
		wpa_printf(MSG_DEBUG, "Not associated - Delay processing of "
			   "received EAPOL frame");
		wpabuf_free(wpa_s->pending_eapol_rx);
		wpa_s->pending_eapol_rx = wpabuf_alloc_copy(buf, len);
		if (wpa_s->pending_eapol_rx) {
			os_get_time(&wpa_s->pending_eapol_rx_time);
			os_memcpy(wpa_s->pending_eapol_rx_src, src_addr,
				  ETH_ALEN);
		}
		return;
	}

	if (wpa_s->key_mgmt == WPA_KEY_MGMT_NONE) {
		wpa_printf(MSG_DEBUG, "Ignored received EAPOL frame since "
			   "no key management is configured");
		return;
	}

	if (wpa_s->eapol_received == 0 &&
	    (!wpa_s->driver_4way_handshake ||
	     !wpa_key_mgmt_wpa_psk(wpa_s->key_mgmt) ||
	     wpa_s->wpa_state != WPA_COMPLETED)) {
		/* Timeout for completing IEEE 802.1X and WPA authentication */
		wpa_supplicant_req_auth_timeout(
			wpa_s,
			(wpa_key_mgmt_wpa_ieee8021x(wpa_s->key_mgmt) ||
			 wpa_s->key_mgmt == WPA_KEY_MGMT_IEEE8021X_NO_WPA ||
			 wpa_s->key_mgmt == WPA_KEY_MGMT_WPS) ?
			70 : 10, 0);
	}
	wpa_s->eapol_received++;

	if (wpa_s->countermeasures) {
		wpa_printf(MSG_INFO, "WPA: Countermeasures - dropped EAPOL "
			   "packet");
		return;
	}

	/* Source address of the incoming EAPOL frame could be compared to the
	 * current BSSID. However, it is possible that a centralized
	 * Authenticator could be using another MAC address than the BSSID of
	 * an AP, so just allow any address to be used for now. The replies are
	 * still sent to the current BSSID (if available), though. */

	os_memcpy(wpa_s->last_eapol_src, src_addr, ETH_ALEN);
	if (!wpa_key_mgmt_wpa_psk(wpa_s->key_mgmt) &&
	    eapol_sm_rx_eapol(wpa_s->eapol, src_addr, buf, len) > 0)
		return;
	wpa_drv_poll(wpa_s);
	if (!wpa_s->driver_4way_handshake)
		wpa_sm_rx_eapol(wpa_s->wpa, src_addr, buf, len);
	else if (wpa_key_mgmt_wpa_ieee8021x(wpa_s->key_mgmt)) {
		/*
		 * Set portValid = TRUE here since we are going to skip 4-way
		 * handshake processing which would normally set portValid. We
		 * need this to allow the EAPOL state machines to be completed
		 * without going through EAPOL-Key handshake.
		 */
		eapol_sm_notify_portValid(wpa_s->eapol, TRUE);
	}
}


void wpa_supplicant_sta_free_hw_features(struct wpa_hw_modes *hw_features,
					 size_t num_hw_features)
{
	ieee80211_sta_free_hw_features(hw_features, num_hw_features);
}


void wpa_supplicant_sta_rx(void *ctx, const u8 *buf, size_t len,
			   struct ieee80211_rx_status *rx_status)
{
	struct wpa_supplicant *wpa_s = ctx;
	ieee80211_sta_rx(wpa_s, buf, len, rx_status);
}


/**
 * wpa_supplicant_driver_init - Initialize driver interface parameters
 * @wpa_s: Pointer to wpa_supplicant data
 * Returns: 0 on success, -1 on failure
 *
 * This function is called to initialize driver interface parameters.
 * wpa_drv_init() must have been called before this function to initialize the
 * driver interface.
 */
int wpa_supplicant_driver_init(struct wpa_supplicant *wpa_s)
{
	static int interface_count = 0;

	if (wpa_s->driver->send_eapol) {
		const u8 *addr = wpa_drv_get_mac_addr(wpa_s);
		if (addr)
			os_memcpy(wpa_s->own_addr, addr, ETH_ALEN);
	} else {
		wpa_s->l2 = l2_packet_init(wpa_s->ifname,
					   wpa_drv_get_mac_addr(wpa_s),
					   ETH_P_EAPOL,
					   wpa_supplicant_rx_eapol, wpa_s, 0);
		if (wpa_s->l2 == NULL)
			return -1;
	}

	if (wpa_s->l2 && l2_packet_get_own_addr(wpa_s->l2, wpa_s->own_addr)) {
		wpa_printf(MSG_ERROR, "Failed to get own L2 address");
		return -1;
	}
#ifdef CONFIG_WAPI_SUPPORT
	wpa_s->l2_wai = l2_packet_init(wpa_s->ifname,
					   wpa_drv_get_mac_addr(wpa_s),
					   ETH_P_WAI,
					   wpa_supplicant_rx_wai, wpa_s, 0);

	if (wpa_s->l2_wai && l2_packet_get_own_addr(wpa_s->l2_wai, wpa_s->own_addr)) {
		wpa_printf(MSG_ERROR, "[WAPI !!>_<!!] Failed to get own L2 address for WAPI");
		return -1;
	}
#endif

	wpa_printf(MSG_DEBUG, "Own MAC address: " MACSTR,
		   MAC2STR(wpa_s->own_addr));

	if (wpa_s->bridge_ifname[0]) {
		wpa_printf(MSG_DEBUG, "Receiving packets from bridge interface"
			   " '%s'", wpa_s->bridge_ifname);
		wpa_s->l2_br = l2_packet_init(wpa_s->bridge_ifname,
					      wpa_s->own_addr,
					      ETH_P_EAPOL,
					      wpa_supplicant_rx_eapol, wpa_s,
					      0);
		if (wpa_s->l2_br == NULL) {
			wpa_printf(MSG_ERROR, "Failed to open l2_packet "
				   "connection for the bridge interface '%s'",
				   wpa_s->bridge_ifname);
			return -1;
		}
	}

	/* Backwards compatibility call to set_wpa() handler. This is called
	 * only just after init and just before deinit, so these handler can be
	 * used to implement same functionality. */
	if (wpa_drv_set_wpa(wpa_s, 1) < 0) {
		struct wpa_driver_capa capa;
		if (wpa_drv_get_capa(wpa_s, &capa) < 0 ||
		    !(capa.flags & (WPA_DRIVER_CAPA_KEY_MGMT_WPA |
				    WPA_DRIVER_CAPA_KEY_MGMT_WPA2))) {
			wpa_printf(MSG_DEBUG, "Driver does not support WPA.");
			/* Continue to allow non-WPA modes to be used. */
		} else {
			wpa_printf(MSG_ERROR, "Failed to enable WPA in the "
				"driver.");
			return -1;
		}
	}

	wpa_clear_keys(wpa_s, NULL);

	/* Make sure that TKIP countermeasures are not left enabled (could
	 * happen if wpa_supplicant is killed during countermeasures. */
	wpa_drv_set_countermeasures(wpa_s, 0);

	wpa_drv_set_drop_unencrypted(wpa_s, 1);

	wpa_printf(MSG_DEBUG, "RSN: flushing PMKID list in the driver");
	wpa_drv_flush_pmkid(wpa_s);

	wpa_s->prev_scan_ssid = BROADCAST_SSID_SCAN;
	wpa_supplicant_req_scan(wpa_s, interface_count, 100000);
	interface_count++;

	return 0;
}


static int wpa_supplicant_daemon(const char *pid_file)
{
	wpa_printf(MSG_DEBUG, "Daemonize..");
	return os_daemonize(pid_file);
}


static struct wpa_supplicant * wpa_supplicant_alloc(void)
{
	struct wpa_supplicant *wpa_s;

	wpa_s = os_zalloc(sizeof(*wpa_s));
	if (wpa_s == NULL)
		return NULL;
	wpa_s->scan_req = 1;
#ifdef ANDROID
	wpa_s->scan_interval = 5;
#endif
	return wpa_s;
}


static int wpa_supplicant_init_iface(struct wpa_supplicant *wpa_s,
				     struct wpa_interface *iface)
{
	wpa_printf(MSG_DEBUG, "Initializing interface '%s' conf '%s' driver "
		   "'%s' ctrl_interface '%s' bridge '%s'", iface->ifname,
		   iface->confname ? iface->confname : "N/A",
		   iface->driver ? iface->driver : "default",
		   iface->ctrl_interface ? iface->ctrl_interface : "N/A",
		   iface->bridge_ifname ? iface->bridge_ifname : "N/A");

	if (wpa_supplicant_set_driver(wpa_s, iface->driver) < 0) {
		return -1;
	}

	if (iface->confname) {
#ifdef CONFIG_BACKEND_FILE
		wpa_s->confname = os_rel2abs_path(iface->confname);
		if (wpa_s->confname == NULL) {
			wpa_printf(MSG_ERROR, "Failed to get absolute path "
				   "for configuration file '%s'.",
				   iface->confname);
			return -1;
		}
		wpa_printf(MSG_DEBUG, "Configuration file '%s' -> '%s'",
			   iface->confname, wpa_s->confname);
#else /* CONFIG_BACKEND_FILE */
		wpa_s->confname = os_strdup(iface->confname);
#endif /* CONFIG_BACKEND_FILE */
		wpa_s->conf = wpa_config_read(wpa_s->confname);
		if (wpa_s->conf == NULL) {
			wpa_printf(MSG_ERROR, "Failed to read or parse "
				   "configuration '%s'.", wpa_s->confname);
			return -1;
		}

		/*
		 * Override ctrl_interface and driver_param if set on command
		 * line.
		 */
		if (iface->ctrl_interface) {
			os_free(wpa_s->conf->ctrl_interface);
			wpa_s->conf->ctrl_interface =
				os_strdup(iface->ctrl_interface);
		}

		if (iface->driver_param) {
			os_free(wpa_s->conf->driver_param);
			wpa_s->conf->driver_param =
				os_strdup(iface->driver_param);
		}
	} else
		wpa_s->conf = wpa_config_alloc_empty(iface->ctrl_interface,
						     iface->driver_param);

	if (wpa_s->conf == NULL) {
		wpa_printf(MSG_ERROR, "\nNo configuration found.");
		return -1;
	}

	if (iface->ifname == NULL) {
		wpa_printf(MSG_ERROR, "\nInterface name is required.");
		return -1;
	}
	if (os_strlen(iface->ifname) >= sizeof(wpa_s->ifname)) {
		wpa_printf(MSG_ERROR, "\nToo long interface name '%s'.",
			   iface->ifname);
		return -1;
	}
	os_strlcpy(wpa_s->ifname, iface->ifname, sizeof(wpa_s->ifname));

	if (iface->bridge_ifname) {
		if (os_strlen(iface->bridge_ifname) >=
		    sizeof(wpa_s->bridge_ifname)) {
			wpa_printf(MSG_ERROR, "\nToo long bridge interface "
				   "name '%s'.", iface->bridge_ifname);
			return -1;
		}
		os_strlcpy(wpa_s->bridge_ifname, iface->bridge_ifname,
			   sizeof(wpa_s->bridge_ifname));
	}

	return 0;
}


static int wpa_supplicant_init_iface2(struct wpa_supplicant *wpa_s)
{
	const char *ifname;
	struct wpa_driver_capa capa;

	wpa_printf(MSG_DEBUG, "Initializing interface (2) '%s'",
		   wpa_s->ifname);

	/* RSNA Supplicant Key Management - INITIALIZE */
	eapol_sm_notify_portEnabled(wpa_s->eapol, FALSE);
	eapol_sm_notify_portValid(wpa_s->eapol, FALSE);

	/* Initialize driver interface and register driver event handler before
	 * L2 receive handler so that association events are processed before
	 * EAPOL-Key packets if both become available for the same select()
	 * call. */
	wpa_s->drv_priv = wpa_drv_init(wpa_s, wpa_s->ifname);
	if (wpa_s->drv_priv == NULL) {
		wpa_printf(MSG_ERROR, "Failed to initialize driver interface");
		return -1;
	}
	if (wpa_drv_set_param(wpa_s, wpa_s->conf->driver_param) < 0) {
		wpa_printf(MSG_ERROR, "Driver interface rejected "
			   "driver_param '%s'", wpa_s->conf->driver_param);
		return -1;
	}

	ifname = wpa_drv_get_ifname(wpa_s);
	if (ifname && os_strcmp(ifname, wpa_s->ifname) != 0) {
		wpa_printf(MSG_DEBUG, "Driver interface replaced interface "
			   "name with '%s'", ifname);
		os_strlcpy(wpa_s->ifname, ifname, sizeof(wpa_s->ifname));
	}

	if (wpa_supplicant_init_wpa(wpa_s) < 0)
		return -1;

	wpa_sm_set_ifname(wpa_s->wpa, wpa_s->ifname,
			  wpa_s->bridge_ifname[0] ? wpa_s->bridge_ifname :
			  NULL);
	wpa_sm_set_fast_reauth(wpa_s->wpa, wpa_s->conf->fast_reauth);

	if (wpa_s->conf->dot11RSNAConfigPMKLifetime &&
	    wpa_sm_set_param(wpa_s->wpa, RSNA_PMK_LIFETIME,
			     wpa_s->conf->dot11RSNAConfigPMKLifetime)) {
		wpa_printf(MSG_ERROR, "Invalid WPA parameter value for "
			   "dot11RSNAConfigPMKLifetime");
		return -1;
	}

	if (wpa_s->conf->dot11RSNAConfigPMKReauthThreshold &&
	    wpa_sm_set_param(wpa_s->wpa, RSNA_PMK_REAUTH_THRESHOLD,
			     wpa_s->conf->dot11RSNAConfigPMKReauthThreshold)) {
		wpa_printf(MSG_ERROR, "Invalid WPA parameter value for "
			"dot11RSNAConfigPMKReauthThreshold");
		return -1;
	}

	if (wpa_s->conf->dot11RSNAConfigSATimeout &&
	    wpa_sm_set_param(wpa_s->wpa, RSNA_SA_TIMEOUT,
			     wpa_s->conf->dot11RSNAConfigSATimeout)) {
		wpa_printf(MSG_ERROR, "Invalid WPA parameter value for "
			   "dot11RSNAConfigSATimeout");
		return -1;
	}

	if (wpa_supplicant_driver_init(wpa_s) < 0)
		return -1;

	if (wpa_s->conf->country[0] && wpa_s->conf->country[1] &&
	    wpa_drv_set_country(wpa_s, wpa_s->conf->country)) {
		wpa_printf(MSG_DEBUG, "Failed to set country");
		return -1;
	}

	wpa_sm_set_own_addr(wpa_s->wpa, wpa_s->own_addr);

	if (wpas_wps_init(wpa_s))
		return -1;

	if (wpa_supplicant_init_eapol(wpa_s) < 0)
		return -1;
	wpa_sm_set_eapol(wpa_s->wpa, wpa_s->eapol);

	wpa_s->ctrl_iface = wpa_supplicant_ctrl_iface_init(wpa_s);
	if (wpa_s->ctrl_iface == NULL) {
		wpa_printf(MSG_ERROR,
			   "Failed to initialize control interface '%s'.\n"
			   "You may have another wpa_supplicant process "
			   "already running or the file was\n"
			   "left by an unclean termination of wpa_supplicant "
			   "in which case you will need\n"
			   "to manually remove this file before starting "
			   "wpa_supplicant again.\n",
			   wpa_s->conf->ctrl_interface);
		return -1;
	}

	if (wpa_drv_get_capa(wpa_s, &capa) == 0) {
		if (capa.flags & WPA_DRIVER_FLAGS_USER_SPACE_MLME) {
			wpa_s->use_client_mlme = 1;
			if (ieee80211_sta_init(wpa_s))
				return -1;
		}
		if (capa.flags & WPA_DRIVER_FLAGS_4WAY_HANDSHAKE)
			wpa_s->driver_4way_handshake = 1;
	}

#ifdef CONFIG_WAPI_SUPPORT
	if(wpa_supplicant_init_wapi(wpa_s) < 0){
		wpa_printf(MSG_ERROR, "init wapi module fail!!");
		return -1;
	}
#endif

	return 0;
}


static void wpa_supplicant_deinit_iface(struct wpa_supplicant *wpa_s)
{
	if (wpa_s->drv_priv) {
		wpa_supplicant_deauthenticate(wpa_s,
					      WLAN_REASON_DEAUTH_LEAVING);

		/* Backwards compatibility call to set_wpa() handler. This is
		 * called only just after init and just before deinit, so these
		 * handler can be used to implement same functionality. */
		if (wpa_drv_set_wpa(wpa_s, 0) < 0) {
			wpa_printf(MSG_ERROR, "Failed to disable WPA in the "
				   "driver.");
		}

		wpa_drv_set_drop_unencrypted(wpa_s, 0);
		wpa_drv_set_countermeasures(wpa_s, 0);
		wpa_clear_keys(wpa_s, NULL);
	}

	wpas_dbus_unregister_iface(wpa_s);

	wpa_supplicant_cleanup(wpa_s);

	if (wpa_s->drv_priv)
		wpa_drv_deinit(wpa_s);
}


/**
 * wpa_supplicant_add_iface - Add a new network interface
 * @global: Pointer to global data from wpa_supplicant_init()
 * @iface: Interface configuration options
 * Returns: Pointer to the created interface or %NULL on failure
 *
 * This function is used to add new network interfaces for %wpa_supplicant.
 * This can be called before wpa_supplicant_run() to add interfaces before the
 * main event loop has been started. In addition, new interfaces can be added
 * dynamically while %wpa_supplicant is already running. This could happen,
 * e.g., when a hotplug network adapter is inserted.
 */
struct wpa_supplicant * wpa_supplicant_add_iface(struct wpa_global *global,
						 struct wpa_interface *iface)
{
	struct wpa_supplicant *wpa_s;

	if (global == NULL || iface == NULL)
		return NULL;

	wpa_s = wpa_supplicant_alloc();
	if (wpa_s == NULL)
		return NULL;

	wpa_s->global = global;

	if (wpa_supplicant_init_iface(wpa_s, iface) ||
	    wpa_supplicant_init_iface2(wpa_s)) {
		wpa_printf(MSG_DEBUG, "Failed to add interface %s",
			   iface->ifname);
		wpa_supplicant_deinit_iface(wpa_s);
		os_free(wpa_s);
		return NULL;
	}

	/* Register the interface with the dbus control interface */
	if (wpas_dbus_register_iface(wpa_s)) {
		wpa_supplicant_deinit_iface(wpa_s);
		os_free(wpa_s);
		return NULL;
	}

#ifdef ANDROID
    char scan_prop[PROPERTY_VALUE_MAX];
    char *endp;
    if (property_get("wifi.supplicant_scan_interval", scan_prop, "6") != 0) {
        wpa_s->scan_interval = (int)strtol(scan_prop, &endp, 0);
        if (endp == scan_prop) {
            wpa_s->scan_interval = 15;
        }
    }
#endif
	wpa_s->next = global->ifaces;
	global->ifaces = wpa_s;

	wpa_printf(MSG_DEBUG, "Added interface %s", wpa_s->ifname);

	return wpa_s;
}


/**
 * wpa_supplicant_remove_iface - Remove a network interface
 * @global: Pointer to global data from wpa_supplicant_init()
 * @wpa_s: Pointer to the network interface to be removed
 * Returns: 0 if interface was removed, -1 if interface was not found
 *
 * This function can be used to dynamically remove network interfaces from
 * %wpa_supplicant, e.g., when a hotplug network adapter is ejected. In
 * addition, this function is used to remove all remaining interfaces when
 * %wpa_supplicant is terminated.
 */
int wpa_supplicant_remove_iface(struct wpa_global *global,
				struct wpa_supplicant *wpa_s)
{
	struct wpa_supplicant *prev;

	/* Remove interface from the global list of interfaces */
	prev = global->ifaces;
	if (prev == wpa_s) {
		global->ifaces = wpa_s->next;
	} else {
		while (prev && prev->next != wpa_s)
			prev = prev->next;
		if (prev == NULL)
			return -1;
		prev->next = wpa_s->next;
	}

	wpa_printf(MSG_DEBUG, "Removing interface %s", wpa_s->ifname);

	wpa_supplicant_deinit_iface(wpa_s);
	os_free(wpa_s);

	return 0;
}


/**
 * wpa_supplicant_get_iface - Get a new network interface
 * @global: Pointer to global data from wpa_supplicant_init()
 * @ifname: Interface name
 * Returns: Pointer to the interface or %NULL if not found
 */
struct wpa_supplicant * wpa_supplicant_get_iface(struct wpa_global *global,
						 const char *ifname)
{
	struct wpa_supplicant *wpa_s;

	for (wpa_s = global->ifaces; wpa_s; wpa_s = wpa_s->next) {
		if (os_strcmp(wpa_s->ifname, ifname) == 0)
			return wpa_s;
	}
	return NULL;
}


/**
 * wpa_supplicant_init - Initialize %wpa_supplicant
 * @params: Parameters for %wpa_supplicant
 * Returns: Pointer to global %wpa_supplicant data, or %NULL on failure
 *
 * This function is used to initialize %wpa_supplicant. After successful
 * initialization, the returned data pointer can be used to add and remove
 * network interfaces, and eventually, to deinitialize %wpa_supplicant.
 */
struct wpa_global * wpa_supplicant_init(struct wpa_params *params)
{
	struct wpa_global *global;
	int ret, i;

	if (params == NULL)
		return NULL;

	wpa_debug_open_file(params->wpa_debug_file_path);

	ret = eap_peer_register_methods();
	if (ret) {
		wpa_printf(MSG_ERROR, "Failed to register EAP methods");
		if (ret == -2)
			wpa_printf(MSG_ERROR, "Two or more EAP methods used "
				   "the same EAP type.");
		return NULL;
	}

	global = os_zalloc(sizeof(*global));
	if (global == NULL)
		return NULL;
	global->params.daemonize = params->daemonize;
	global->params.wait_for_monitor = params->wait_for_monitor;
	global->params.dbus_ctrl_interface = params->dbus_ctrl_interface;
	if (params->pid_file)
		global->params.pid_file = os_strdup(params->pid_file);
	if (params->ctrl_interface)
		global->params.ctrl_interface =
			os_strdup(params->ctrl_interface);
	wpa_debug_level = global->params.wpa_debug_level =
		params->wpa_debug_level;
	wpa_debug_show_keys = global->params.wpa_debug_show_keys =
		params->wpa_debug_show_keys;
	wpa_debug_timestamp = global->params.wpa_debug_timestamp =
		params->wpa_debug_timestamp;

	if (eloop_init(global)) {
		wpa_printf(MSG_ERROR, "Failed to initialize event loop");
		wpa_supplicant_deinit(global);
		return NULL;
	}

	global->ctrl_iface = wpa_supplicant_global_ctrl_iface_init(global);
	if (global->ctrl_iface == NULL) {
		wpa_supplicant_deinit(global);
		return NULL;
	}

	if (global->params.dbus_ctrl_interface) {
		global->dbus_ctrl_iface =
			wpa_supplicant_dbus_ctrl_iface_init(global);
		if (global->dbus_ctrl_iface == NULL) {
			wpa_supplicant_deinit(global);
			return NULL;
		}
	}

	for (i = 0; wpa_supplicant_drivers[i]; i++)
		global->drv_count++;
	if (global->drv_count == 0) {
		wpa_printf(MSG_ERROR, "No drivers enabled");
		wpa_supplicant_deinit(global);
		return NULL;
	}
	global->drv_priv = os_zalloc(global->drv_count * sizeof(void *));
	if (global->drv_priv == NULL) {
		wpa_supplicant_deinit(global);
		return NULL;
	}
	for (i = 0; wpa_supplicant_drivers[i]; i++) {
		if (!wpa_supplicant_drivers[i]->global_init)
			continue;
		global->drv_priv[i] = wpa_supplicant_drivers[i]->global_init();
		if (global->drv_priv[i] == NULL) {
			wpa_printf(MSG_ERROR, "Failed to initialize driver "
				   "'%s'", wpa_supplicant_drivers[i]->name);
			wpa_supplicant_deinit(global);
			return NULL;
		}
	}

	return global;
}


/**
 * wpa_supplicant_run - Run the %wpa_supplicant main event loop
 * @global: Pointer to global data from wpa_supplicant_init()
 * Returns: 0 after successful event loop run, -1 on failure
 *
 * This function starts the main event loop and continues running as long as
 * there are any remaining events. In most cases, this function is running as
 * long as the %wpa_supplicant process in still in use.
 */
int wpa_supplicant_run(struct wpa_global *global)
{
	struct wpa_supplicant *wpa_s;

	if (global->params.daemonize &&
	    wpa_supplicant_daemon(global->params.pid_file))
		return -1;

	if (global->params.wait_for_monitor) {
		for (wpa_s = global->ifaces; wpa_s; wpa_s = wpa_s->next)
			if (wpa_s->ctrl_iface)
				wpa_supplicant_ctrl_iface_wait(
					wpa_s->ctrl_iface);
	}

	eloop_register_signal_terminate(wpa_supplicant_terminate, NULL);
	eloop_register_signal_reconfig(wpa_supplicant_reconfig, NULL);

	eloop_run();

	return 0;
}


/**
 * wpa_supplicant_deinit - Deinitialize %wpa_supplicant
 * @global: Pointer to global data from wpa_supplicant_init()
 *
 * This function is called to deinitialize %wpa_supplicant and to free all
 * allocated resources. Remaining network interfaces will also be removed.
 */
void wpa_supplicant_deinit(struct wpa_global *global)
{
	int i;

	if (global == NULL)
		return;

	wpa_supplicant_terminate(0, global, NULL);

	while (global->ifaces)
		wpa_supplicant_remove_iface(global, global->ifaces);

	if (global->ctrl_iface)
		wpa_supplicant_global_ctrl_iface_deinit(global->ctrl_iface);
	if (global->dbus_ctrl_iface)
		wpa_supplicant_dbus_ctrl_iface_deinit(global->dbus_ctrl_iface);

	eap_peer_unregister_methods();

	for (i = 0; wpa_supplicant_drivers[i] && global->drv_priv; i++) {
		if (!global->drv_priv[i])
			continue;
		wpa_supplicant_drivers[i]->global_deinit(global->drv_priv[i]);
	}
	os_free(global->drv_priv);

	eloop_destroy();

	if (global->params.pid_file) {
		os_daemonize_terminate(global->params.pid_file);
		os_free(global->params.pid_file);
	}
	os_free(global->params.ctrl_interface);

	os_free(global);
	wpa_debug_close_file();
}
