/*
 * Copyright (C) 2008 Torch Mobile Inc. All rights reserved. (http://www.torchmobile.com/)
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
 *
 */

#include "config.h"

#if ENABLE(WML)
#include "WMLDocument.h"

#include "BackForwardController.h"
#include "BackForwardList.h"
#include "Frame.h"
#include "Page.h"
#include "ScriptableDocumentParser.h"
#include "WMLCardElement.h"
#include "WMLErrorHandling.h"
#include "WMLPageState.h"
#include "WMLTemplateElement.h"

namespace WebCore {

WMLDocument::WMLDocument(Frame* frame, const KURL& url)
    : Document(frame, url, false, false) 
    , m_activeCard(0)
    /// M: ALPS00439551 use a timer to trigger handleIntrinsicEventIfNeeded() @{
    , m_intrinsicEventTimer(this, &WMLDocument::handleIntrinsicEventTimerFired)
    , m_policyDownloadError(false)
    /// @}
{
    clearXMLVersion();
}

WMLDocument::~WMLDocument()
{
}

void WMLDocument::finishedParsing()
{
    if (ScriptableDocumentParser* parser = this->scriptableDocumentParser()) {
        if (!parser->wellFormed()) {
            Document::finishedParsing();
            return;
        }
    }

    bool hasAccess = initialize(true);
    Document::finishedParsing();

    if (!hasAccess) {
        m_activeCard = 0;

        WMLPageState* wmlPageState = wmlPageStateForDocument(this);
        if (!wmlPageState)
            return;

        Page* page = wmlPageState->page();
        if (!page)
            return;

        HistoryItem* item = page->backForward()->backItem();
        if (!item)
            return;

        page->goToItem(item, FrameLoadTypeBackWMLDeckNotAccessible);
        return;
    }

    /// M: ALPS00439551 set a flag to prevent infinite loop in WMLDocument @{
    if (m_policyDownloadError) {
        m_policyDownloadError = false;
        return;
    }
    /// @}

    /// M: ALPS00439551 use a timer to trigger handleIntrinsicEventIfNeeded()
    m_intrinsicEventTimer.startOneShot(0.0f);
}

/// M: ALPS00439551 set a flag to prevent infinite loop in WMLDocument @{
void WMLDocument::onPolicyDownloadError()
{
    m_policyDownloadError = true;
}

void WMLDocument::handleIntrinsicEventTimerFired(Timer<WMLDocument>*)
{
    if (m_activeCard)
        m_activeCard->handleIntrinsicEventIfNeeded();
}
/// @}

bool WMLDocument::initialize(bool aboutToFinishParsing)
{
    WMLPageState* wmlPageState = wmlPageStateForDocument(this);
    if (!wmlPageState || !wmlPageState->canAccessDeck())
        return false;

    // Remember that we'e successfully entered the deck
    wmlPageState->resetAccessControlData();

    // Notify the existance of templates to all cards of the current deck
    WMLTemplateElement::registerTemplatesInDocument(this);

    // Set destination card
    m_activeCard = WMLCardElement::determineActiveCard(this);
    if (!m_activeCard) {
        reportWMLError(this, WMLErrorNoCardInDocument);
        return true;
    }

    // Handle deck-level task overrides
    m_activeCard->handleDeckLevelTaskOverridesIfNeeded();

    // Handle card-level intrinsic event
    if (!aboutToFinishParsing)
        m_activeCard->handleIntrinsicEventIfNeeded();

    return true;
}

WMLPageState* wmlPageStateForDocument(Document* doc)
{
    ASSERT(doc);

    Page* page = doc->page();
    if (!page)
        return (WMLPageState*)0;

    return page->wmlPageState();
}

}

#endif
