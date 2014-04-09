/*
 * Copyright (C) 2008 The Android Open Source Project
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

#ifndef _COMMANDLISTENER_H__
#define _COMMANDLISTENER_H__

#include <sysutils/FrameworkListener.h>

#include "NetdCommand.h"
#include "TetherController.h"
#include "NatController.h"
#include "PppController.h"
#ifdef MTK_WLAN_SUPPORT
#include "SoftapController_mtk.h"
#else
#include "SoftapController.h"
#endif
#include "BandwidthController.h"
#include "IdletimerController.h"
#include "InterfaceController.h"
#include "ResolverController.h"
#include "MultiRouterController.h"
#include "SecondaryTableController.h"
#include "ThroughputMonitor.h"
#include "ThrottleController.h"
#include "IPv6TetherController.h"
#include "FirewallController.h"

class CommandListener : public FrameworkListener {
    static TetherController *sTetherCtrl;
    static NatController *sNatCtrl;
    static PppController *sPppCtrl;
    static SoftapController *sSoftapCtrl;
    static BandwidthController *sBandwidthCtrl;
    static IdletimerController *sIdletimerCtrl;
    static InterfaceController *sInterfaceCtrl;
    static ResolverController *sResolverCtrl;
    static MultiRouterController *sMultiRouterCtrl;
    static SecondaryTableController *sSecondaryTableCtrl;
    static IPv6TetherController *sIPv6TetherCtrl;
    static ThroughputMonitor *sThroughputMonitor;
	static ThrottleController *sThrottleCtrl;
    static FirewallController *sFirewallCtrl;

public:
    CommandListener();
    virtual ~CommandListener() {}
	static ThrottleController * getThrottleCtrl() {return sThrottleCtrl; }

private:

    static int writeFile(const char *path, const char *value, int size);

    static int readInterfaceCounters(const char *iface, unsigned long *rx, unsigned long *tx);

    static int DumpRoute();
    class SoftapCmd : public NetdCommand {
    public:
        SoftapCmd();
        virtual ~SoftapCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };

    class InterfaceCmd : public NetdCommand {
    public:
        InterfaceCmd();
        virtual ~InterfaceCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };

    class IpFwdCmd : public NetdCommand {
    public:
        IpFwdCmd();
        virtual ~IpFwdCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };

    class TetherCmd : public NetdCommand {
    public:
        TetherCmd();
        virtual ~TetherCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };

    class NatCmd : public NetdCommand {
    public:
        NatCmd();
        virtual ~NatCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };

    class ListTtysCmd : public NetdCommand {
    public:
        ListTtysCmd();
        virtual ~ListTtysCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };

    class PppdCmd : public NetdCommand {
    public:
        PppdCmd();
        virtual ~PppdCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };

    class BandwidthControlCmd : public NetdCommand {
    public:
        BandwidthControlCmd();
        virtual ~BandwidthControlCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    protected:
        void sendGenericOkFail(SocketClient *cli, int cond);
        void sendGenericOpFailed(SocketClient *cli, const char *errMsg);
        void sendGenericSyntaxError(SocketClient *cli, const char *usageMsg);
    };

    class IdletimerControlCmd : public NetdCommand {
    public:
        IdletimerControlCmd();
        virtual ~IdletimerControlCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };

    class ResolverCmd : public NetdCommand {
    public:
        ResolverCmd();
        virtual ~ResolverCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };
	
	class MultiRouterCmd : public NetdCommand {
    public:
        MultiRouterCmd();
        virtual ~MultiRouterCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    };
	
	class Ipv6FwdCmd : public NetdCommand {
	public:
	    Ipv6FwdCmd();
	    virtual ~Ipv6FwdCmd() {}
	    int runCommand(SocketClient *c, int argc, char ** argv);
	};
	
	class IPv6TetherCmd : public NetdCommand {
	public:
	    IPv6TetherCmd();
	    virtual ~IPv6TetherCmd() {}
	    int runCommand(SocketClient *c, int argc, char ** argv);
	};
	
	class ThroughputCmd : public NetdCommand {
	public:
	    ThroughputCmd();
	    virtual ~ThroughputCmd() {}
	    int runCommand(SocketClient *c, int argc, char ** argv);
	};

    class FirewallCmd: public NetdCommand {
    public:
        FirewallCmd();
        virtual ~FirewallCmd() {}
        int runCommand(SocketClient *c, int argc, char ** argv);
    protected:
        int sendGenericOkFail(SocketClient *cli, int cond);
        static FirewallRule parseRule(const char* arg);
    };
};
#endif
