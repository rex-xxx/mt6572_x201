#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# *MTK* We use prebuild CTS for compatibilty certification test.
# The CTS code here is just for reference
# Disable CTS build will boot up android ROM build
# Set BUILD_CTS=yes/no in ProjectConfig.mk to enable/disable CTS build
ifneq ($(BUILD_CTS),no)
include cts/CtsBuild.mk
include cts/CtsCoverage.mk
include $(call all-subdir-makefiles)
endif
