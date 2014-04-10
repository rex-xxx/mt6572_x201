/*
 * Copyright (C) 1999 Lars Knoll (knoll@kde.org)
 *           (C) 1999 Antti Koivisto (koivisto@kde.org)
 *           (C) 2001 Dirk Mueller (mueller@kde.org)
 *           (C) 2006 Alexey Proskuryakov (ap@webkit.org)
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2011 Apple Inc. All rights reserved.
 * Copyright (C) 2008, 2009 Torch Mobile Inc. All rights reserved. (http://www.torchmobile.com/)
 * Copyright (C) 2008, 2009 Google Inc. All rights reserved.
 * Copyright (C) 2010 Nokia Corporation and/or its subsidiary(-ies)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#include "config.h"
#include "PageVisibilityState.h"

#if ENABLE(PAGE_VISIBILITY_API)

namespace WebCore {

String pageVisibilityStateString(PageVisibilityState state)
{
    DEFINE_STATIC_LOCAL(const String, visible, ("visible"));
    DEFINE_STATIC_LOCAL(const String, hidden, ("hidden"));
    DEFINE_STATIC_LOCAL(const String, prerender, ("prerender"));
    DEFINE_STATIC_LOCAL(const String, preview, ("preview"));

    switch (state) {
    case PageVisibilityStateVisible:
        return visible;
    case PageVisibilityStateHidden:
        return hidden;
    case PageVisibilityStatePrerender:
        return prerender;
    case PageVisibilityStatePreview:
        return preview;
    }

    ASSERT_NOT_REACHED();
    return String();
}

} // namespace WebCore

#endif // if ENABLE(PAGE_VISIBILITY_API)

