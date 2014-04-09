/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <pthread.h>
//#include <cutils/log.h>
#include <cutils/properties.h>
#include "custom_prop.h"

typedef struct _ItemHead{
    struct _ItemHead *next;
    struct _ItemHead *prev;
} ItemHead;

typedef struct _ItemNode {
    ItemHead head;
    char module[MAX_MODULE_LEN];
    char key[MAX_KEY_LEN];
    char value[MAX_VALUE_LEN];
    char prop[PROPERTY_KEY_MAX];
} ItemNode;

typedef enum {
    STATE_PREFIX,
    STATE_KEY,
    STATE_VALUE
} ParseLineState;

typedef struct {
    int  lineState;
    char *ptr;
    char *prop;
    ItemNode *currItem;
} Parser;

static ItemNode confItems[MAX_ITEM_NUM+1];
static ItemHead itemList;
static Parser confParser;
static int initDone;
static pthread_mutex_t parseMutex = PTHREAD_MUTEX_INITIALIZER;

static ItemNode *add_item()
{
    ItemNode *node;
    int i;

    for (i = 0; i < MAX_ITEM_NUM; i++) {
        if (!confItems[i].head.next && !confItems[i].head.prev) {
            memset(&confItems[i], 0, sizeof(confItems[0]));
            break;
        }
    }

    if (i == MAX_ITEM_NUM)
        return 0;

    if (!itemList.prev) {
        itemList.prev = itemList.next = &itemList;
    }

    confItems[i].head.next = &itemList;
    confItems[i].head.prev = itemList.prev;
    itemList.prev->next = &confItems[i].head;
    itemList.prev = &confItems[i].head;

    return &confItems[i];
}

static void del_item(ItemHead *item)
{
    if (!itemList.prev || itemList.prev == &itemList)
        return;

    item->prev->next = item->next;
    item->next->prev = item->prev;
    item->next = item->prev = 0;
}

static ItemNode *find_item(const char *module, const char *key)
{
    ItemNode *item, *find = 0;

    if (!itemList.prev || itemList.prev == &itemList)
        return 0;

    for (item = (ItemNode *)itemList.next; &item->head != &itemList; item = (ItemNode *)item->head.next) {
        if (strcmp(item->key, key) == 0) {
            if (!module || strlen(module) == 0)
                return item;
            if (strlen(item->module) == 0 || item->module[0] == '*') {
                find = item;
                continue;
            }
            if (strcmp(item->module, module) == 0)
                return item;
        }
    }

    if (find) return find;

    return 0;
}

static void parser_reset(Parser *parser)
{
    if (parser->currItem) {
        del_item(&parser->currItem->head);
        parser->currItem = 0;
    }
    parser->lineState = STATE_PREFIX;
}

static char *parse_state_prefix(char *ptr, Parser *parser)
{
    switch (*ptr) {
    case '\n':
    case '\r':
    case '\t':
    case ' ':
        break;
    case '#':
        while (*ptr && (*ptr != '\n')) ptr++;
        ptr--;
        break;

    default:
        parser->lineState = STATE_KEY;
        parser->currItem = add_item();
        parser->ptr = parser->currItem->key;
        ptr--;
        break;
    }

    return ptr;
}

static char * parse_state_key(char *ptr, Parser *parser)
{
    ItemNode *item = parser->currItem;

    switch (*ptr) {
    case '\r':
    case '\t':
    case ' ':
        break;
    case '\n':
        parser_reset(parser);
        break;

    case '#':
        parser_reset(parser);
        while (*ptr && (*ptr != '\n')) ptr++;
        ptr--;
        break;

    case '.':
        *parser->ptr = '\0';
        strncpy(item->module, item->key, sizeof(item->module)-1);
        parser->ptr = item->key;

        while (*ptr == '.') ptr++;
        if (*ptr != '.')
            ptr--;
        break;

    case '=':
        parser->lineState = STATE_VALUE;
        *parser->ptr = '\0';
        parser->ptr = item->value;
        break;

    default:
        *parser->ptr++ = *ptr;
        break;
    }
    
    return ptr;
}

static char *parse_state_value(char *ptr, Parser *parser)
{
    ItemNode *item = parser->currItem;
    int len;

    switch (*ptr) {
    case '\r':
        break;
    case '\t':
    case ' ':
        if (parser->ptr != item->value) {
            *parser->ptr++ = *ptr;
        }
        break;
    case '\n':
        *parser->ptr = '\0';
        parser->currItem = 0;
        parser->lineState = STATE_PREFIX;
        break;

    case '@':
        parser->prop = item->prop;
        ptr++;
        while (*ptr && (*ptr != '\n') && (*ptr != ' ') && (*ptr != '\t')) {
            *parser->prop++ = *ptr++;
        }
        *parser->prop = '\0';
        len = property_get(item->prop, parser->ptr, 0);
        if (len > 0)
            parser->ptr += len;
        if (!ptr || *ptr == '\n')
            ptr--;

        break;

    case '#':
        while (*ptr && (*ptr != '\n')) ptr++;
        ptr--;
        break;

    default:
        *parser->ptr++ = *ptr;
        break;
    }
    
    return ptr;
}

static int parse_data(const char *data, Parser *parser)
{
    char *ptr;

    for (ptr = (char *)data; *ptr; ptr++) {
        switch (parser->lineState) {
        case STATE_PREFIX:
            ptr = parse_state_prefix(ptr, parser);
            break;
        case STATE_KEY:
            ptr = parse_state_key(ptr, parser);
            break;
        case STATE_VALUE:
            ptr = parse_state_value(ptr, parser);
            break;
        }
    }

    if (parser->currItem)
    {
        if (parser->lineState == STATE_VALUE)
            *parser->ptr = '\0';
        else
            parser_reset(parser);
    }

    return 1;
}


static int parse_file(const char *name)
{
    int fd, size, result = 0;
    char *data = 0;

    fd = open(name, O_RDONLY);
    if (fd < 0) return 0;

    size = lseek(fd, 0, SEEK_END);
    if (size < 0) goto parse_fail;

    if (lseek(fd, 0, SEEK_SET) != 0)
        goto parse_fail;

    data = (char *)malloc(size+1);
    if (!data) goto parse_fail;

    if (read(fd, data, size) != size)
        goto parse_fail;

    data[size] = '\0';

    result = parse_data(data, &confParser);

parse_fail:
    if (data)
        free(data);
    close(fd);

    return result;
}

int custom_get_string(const char *module, const char *key, char *value, const char *default_value)
{
    ItemNode *node;

    pthread_mutex_lock(&parseMutex);
    if (!initDone) {
        if (parse_file("/system/etc/custom.conf"))
            initDone = 1;
        else {
            pthread_mutex_unlock(&parseMutex);
            goto find_fail;
        }
    }
    pthread_mutex_unlock(&parseMutex);

    node = find_item(module, key);

    if (!node)
        goto find_fail;

    strcpy(value, node->value);

    return strlen(node->value);

find_fail:
    if (default_value) {
        strcpy(value, default_value);
        return strlen(default_value);
    }

    return -1;
}
