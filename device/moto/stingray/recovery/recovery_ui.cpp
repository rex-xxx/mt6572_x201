/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <string.h>
#include <linux/input.h>
#include <cutils/properties.h>

#include "common.h"
#include "device.h"
#include "screen_ui.h"

extern "C" {
    int bp_master_clear(void);
}

const char* HEADERS[] = { "Use volume keys to highlight; power button to select.",
                          "",
                          NULL };

const char* ITEMS[] = { "reboot system now",
                        "apply update from ADB",
                        "apply update from USB drive",
                        "wipe data/factory reset",
                        "wipe cache partition",
                        NULL };

// On stingray, the power key shows up as KEY_END.

class StingrayUI : public ScreenRecoveryUI {
  public:
    StingrayUI() :
        consecutive_power_keys(0) {
    }

    virtual KeyAction CheckKey(int key) {
        if (IsKeyPressed(KEY_END) && key == KEY_VOLUMEUP) {
            return TOGGLE;
        }
        if (key == KEY_POWER) {
            ++consecutive_power_keys;
            if (consecutive_power_keys >= 7) {
                return REBOOT;
            }
        } else {
            consecutive_power_keys = 0;
        }
        return ENQUEUE;
    }

  private:
    int consecutive_power_keys;
};

class StingrayDevice : public Device {
  public:
    StingrayDevice() :
        ui(new StingrayUI) {
    }

    RecoveryUI* GetUI() { return ui; }

    int HandleMenuKey(int key_code, int visible) {
        if (visible) {
            switch (key_code) {
              case KEY_DOWN:
              case KEY_VOLUMEDOWN:
                return kHighlightDown;

              case KEY_UP:
              case KEY_VOLUMEUP:
                return kHighlightUp;

              case KEY_END:
                return kInvokeItem;
            }
        }

        return kNoAction;
    }

    BuiltinAction InvokeMenuItem(int menu_position) {
        switch (menu_position) {
          case 0: return REBOOT;
          case 1: return APPLY_ADB_SIDELOAD;
          case 2: return APPLY_EXT;
          case 3: return WIPE_DATA;
          case 4: return WIPE_CACHE;
          default: return NO_ACTION;
        }
    }

    const char* const* GetMenuHeaders() { return HEADERS; }
    const char* const* GetMenuItems() { return ITEMS; }

    int WipeData() {
        if (device_has_bp()) {
            ui->Print("Performing BP clear...\n");
            if (bp_master_clear() == 0) {
                ui->Print("BP clear complete successfully.\n");
            } else {
                ui->Print("BP clear failed.\n");
            }
        }
        return 0;
    }

  private:
    RecoveryUI* ui;

    bool device_has_bp(void) {
        char value[PROPERTY_VALUE_MAX];

        property_get("ro.carrier", value, "");
        if (strcmp("wifi-only", value) == 0)
            return false;
        else
            return true;
    }
};

Device* make_device() {
    return new StingrayDevice;
}
