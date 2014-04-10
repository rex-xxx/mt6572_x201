/*
 * WPA Supplicant - Glue code to setup EAPOL and RSN modules
 * Copyright (c) 2003-2008, Jouni Malinen <j@w1.fi>
 *
 * This software may be distributed under the terms of the BSD license.
 * See README for more details.
 */

#ifndef WPAS_GLUE_H
#define WPAS_GLUE_H

enum wpa_ctrl_req_type;

int wpa_supplicant_init_eapol(struct wpa_supplicant *wpa_s);
int wpa_supplicant_init_wpa(struct wpa_supplicant *wpa_s);
void wpa_supplicant_rsn_supp_set_config(struct wpa_supplicant *wpa_s,
					struct wpa_ssid *ssid);

const char * wpa_supplicant_ctrl_req_to_string(enum wpa_ctrl_req_type field,
					       const char *default_txt,
					       const char **txt);

enum wpa_ctrl_req_type wpa_supplicant_ctrl_req_from_string(const char *field);
int wpa_ether_send(struct wpa_supplicant *wpa_s, const u8 *dest,			  
                    u16 proto, const u8 *buf, size_t len);

#ifdef CONFIG_WAPI_SUPPORT
int wpa_supplicant_init_wapi(struct wpa_supplicant *wpa_s);
int wpa_supplicant_deinit_wapi(struct wpa_supplicant *wpa_s);
#endif
#endif /* WPAS_GLUE_H */
