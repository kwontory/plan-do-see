/*
 * PlanDoSee Note - progressive enhancement only.
 * Every feature works without this file. Button locking here is a UX aid; duplicate
 * completion is prevented on the server (idempotency key, row lock, unique constraint).
 * Never write user strings through innerHTML; this file only moves existing DOM nodes
 * and sets fixed textContent.
 * Elements are found by behaviour attributes only: data-js="flash",
 * data-js="error-summary", data-js="focus-first-invalid", data-js="busy-label", data-js="delete-confirm",
 * data-js="logout", data-js="draft-note" (+ data-draft-for), data-js="draft-continue", data-js="draft-discard",
 * form[data-draft-id], data-js="add-open", data-js="add-panel" (+ data-open), data-js="add-close",
 * data-js="filter-toggle", data-js="filter-panel" (+ data-open), data-js="state-menu", data-no-lock, data-submitting. data-js may hold several space-separated names (matched with ~=).
 * Logged-in pages carry <meta name="pds-draft-owner"> and <meta name="pds-keepalive"> (ADR-38): the tab drafts and the
 * keepalive request below are off without them (login and sign-up pages).
 * State written here for CSS: aria-busy on a busy button, data-focus-origin="script" on an element focused by this file.
 * Style classes (class) and test hooks (data-test) are never used as selectors here.
 */
(function () {
    'use strict';

    // Busy label comes from messages.properties (common.busy) through <meta name="pds-busy-text">.
    var busyMeta = document.querySelector('meta[name="pds-busy-text"]');
    var BUSY_TEXT = busyMeta ? busyMeta.getAttribute('content') : '...';
    var originals = new WeakMap();

    // The part of a button whose text changes while busy: a [data-js~="busy-label"] inside the button (so an icon or a
    // fixed-size round button keeps its shape), otherwise the whole button.
    function labelOf(button) {
        return button.querySelector('[data-js~="busy-label"]') || button;
    }

    function lock(button) {
        if (!button || originals.has(button)) {
            return;
        }
        var target = labelOf(button);
        var saved = [];
        target.childNodes.forEach(function (node) {
            saved.push(node.cloneNode(true));
        });
        originals.set(button, {target: target, nodes: saved});
        target.textContent = BUSY_TEXT;
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
    }

    function unlock(button) {
        var saved = originals.get(button);
        if (!saved) {
            return;
        }
        saved.target.replaceChildren.apply(saved.target, saved.nodes.map(function (node) {
            return node.cloneNode(true);
        }));
        button.disabled = false;
        button.removeAttribute('aria-busy');
        originals.delete(button);
    }

    // Lock the button that submitted a POST form, so a double click sends one request.
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        if ((form.getAttribute('method') || 'get').toLowerCase() !== 'post') {
            return;
        }
        if (form.dataset.submitting === 'true') {
            event.preventDefault();
            return;
        }
        form.dataset.submitting = 'true';
        var button = event.submitter || form.querySelector('button[type="submit"], button:not([type])');
        if (button && button.hasAttribute('data-no-lock')) {
            return;
        }
        // Defer so the browser has already built the form data set before the button is disabled.
        window.setTimeout(function () {
            lock(button);
        }, 0);
    });

    // Restore buttons when the page comes back from the back/forward cache.
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('form[data-submitting="true"]').forEach(function (form) {
            delete form.dataset.submitting;
            form.querySelectorAll('button').forEach(unlock);
        });
    });

    // Re-announce a flash message that was already present at load (after PRG redirect).
    // Screen readers often skip live-region content that exists before load, so the region is
    // emptied and the same nodes are put back shortly after. Nodes are moved, never re-parsed.
    function reannounceFlash() {
        var flash = document.querySelector('[data-js~="flash"]');
        if (!flash || !flash.textContent.trim()) {
            return;
        }
        reannounce(flash);
    }

    // Re-announce the "not saved" alert (role=alert) that was already in the page at load: like the flash, the
    // nodes are taken out and put back so screen readers read it, while focus stays on the first invalid field.
    function reannounce(region) {
        var nodes = Array.prototype.slice.call(region.childNodes);
        region.replaceChildren();
        window.setTimeout(function () {
            region.replaceChildren.apply(region, nodes);
        }, 150);
    }

    // Save failed: focus the first invalid field in screen order inside a form marked
    // data-js="focus-first-invalid". The field carries aria-invalid="true" and its aria-describedby starts with the
    // error text id, so the field, the error and the help are read together. With no focusable invalid field (only
    // global errors, a conflict, a read-only copy) the alert itself gets focus as before.
    document.addEventListener('DOMContentLoaded', function () {
        var summary = document.querySelector('[data-js~="error-summary"]');
        var invalid = document.querySelector('[data-js~="focus-first-invalid"] [aria-invalid="true"]');
        if (invalid) {
            invalid.focus();
            if (document.activeElement === invalid) {
                if (summary) {
                    reannounce(summary);
                }
                return;
            }
        }
        if (summary) {
            focusQuietly(summary);
            return;
        }
        reannounceFlash();
    });

    // Focus moved by this script (not by the user) is marked with data-focus-origin="script" until the element loses
    // focus, so the CSS can leave out the keyboard focus ring there (the box itself is already prominent). When the user
    // reaches the element with the keyboard later, the attribute is gone and the normal focus ring shows.
    function focusQuietly(element) {
        element.setAttribute('data-focus-origin', 'script');
        element.addEventListener('blur', function () {
            element.removeAttribute('data-focus-origin');
        }, {once: true});
        element.focus();
    }

    // Delete confirmation: Escape closes the open <details> and returns focus to its summary.
    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }
        var details = event.target instanceof Element ? event.target.closest('[data-js~="delete-confirm"]') : null;
        if (!details || !details.open) {
            return;
        }
        details.open = false;
        var summary = details.querySelector('summary');
        if (summary) {
            summary.focus();
        }
    });

    // Todo state dropdown (<details data-js="state-menu">): Escape closes the open menu and puts focus back on its
    // summary; a click outside closes it without moving focus. Opening, closing and submitting work without this.
    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }
        var menu = event.target instanceof Element ? event.target.closest('[data-js~="state-menu"]') : null;
        if (!menu || !menu.open) {
            return;
        }
        menu.open = false;
        var summary = menu.querySelector('summary');
        if (summary) {
            summary.focus();
        }
    });

    document.addEventListener('click', function (event) {
        document.querySelectorAll('[data-js~="state-menu"][open]').forEach(function (menu) {
            if (!(event.target instanceof Node) || !menu.contains(event.target)) {
                menu.open = false;
            }
        });
    });

    // ---------- Tab drafts (ADR-38, common-layout.md 11) ----------
    // A form marked data-draft-id keeps what is typed in sessionStorage (this tab only, gone when the tab closes), so a
    // save refused after the session ended does not lose the text. Passwords, hidden fields (CSRF token, _method,
    // version) and files are never kept. Key: "draft:" + the person's hash (meta pds-draft-owner) + ":" + the form's
    // action path (the same for the empty form and a form shown again after an error) + ":" + data-draft-id.
    // When the form opens and a draft differs from the fields, the note above the form offers "continue" (fill the
    // fields) or "start over" (forget the draft); nothing is filled on its own. Until one is chosen, typing does not
    // overwrite the old draft. A form shown again by the server with its error box already holds the input: no note.
    // A submitted form's draft is forgotten when the next logged-in page shows a result notice (flash) and no error box;
    // the login page (session ended) keeps it. Logout forgets every draft of the tab.
    var ownerMeta = document.querySelector('meta[name="pds-draft-owner"]');
    var OWNER = ownerMeta ? ownerMeta.getAttribute('content') : '';
    var DRAFT_PREFIX = 'draft:';
    var PENDING_KEY = 'draft-pending';
    var SKIPPED_TYPES = ['password', 'hidden', 'file', 'submit', 'button', 'reset', 'image'];
    var SKIPPED_NAMES = ['_csrf', '_method', 'version'];

    function tabStore() {
        try {
            return window.sessionStorage;
        } catch (e) {
            return null;
        }
    }

    function storeGet(key) {
        var store = tabStore();
        try {
            return store ? store.getItem(key) : null;
        } catch (e) {
            return null;
        }
    }

    function storeSet(key, value) {
        var store = tabStore();
        try {
            if (store) {
                store.setItem(key, value);
            }
        } catch (e) {
            // Storage full or blocked: the form still works, only the draft is not kept.
        }
    }

    function storeRemove(key) {
        var store = tabStore();
        try {
            if (store) {
                store.removeItem(key);
            }
        } catch (e) {
            // ignore
        }
    }

    function forgetAllDrafts() {
        var store = tabStore();
        if (!store) {
            return;
        }
        try {
            var keys = [];
            for (var i = 0; i < store.length; i++) {
                var key = store.key(i);
                if (key && (key.indexOf(DRAFT_PREFIX) === 0 || key === PENDING_KEY)) {
                    keys.push(key);
                }
            }
            keys.forEach(function (key) {
                store.removeItem(key);
            });
        } catch (e) {
            // ignore
        }
    }

    function draftable(element) {
        if (!element || !element.name || element.disabled) {
            return false;
        }
        var tag = element.tagName;
        if (tag !== 'INPUT' && tag !== 'TEXTAREA' && tag !== 'SELECT') {
            return false;
        }
        var type = (element.getAttribute('type') || '').toLowerCase();
        return SKIPPED_TYPES.indexOf(type) < 0 && SKIPPED_NAMES.indexOf(element.name) < 0;
    }

    function draftFields(form) {
        return Array.prototype.filter.call(form.elements, draftable);
    }

    // Values by field name, in document order (several boxes may share a name, e.g. checkboxes).
    function snapshot(form) {
        var data = {};
        draftFields(form).forEach(function (element) {
            var list = data[element.name] || (data[element.name] = []);
            var type = (element.getAttribute('type') || '').toLowerCase();
            if (type === 'checkbox' || type === 'radio') {
                list.push(element.checked);
            } else if (element.tagName === 'SELECT' && element.multiple) {
                list.push(Array.prototype.filter.call(element.options, function (option) {
                    return option.selected;
                }).map(function (option) {
                    return option.value;
                }));
            } else {
                list.push(element.value);
            }
        });
        return data;
    }

    function fill(form, data) {
        var seen = {};
        draftFields(form).forEach(function (element) {
            var list = data[element.name];
            if (!Array.isArray(list)) {
                return;
            }
            var index = seen[element.name] || 0;
            seen[element.name] = index + 1;
            if (index >= list.length) {
                return;
            }
            var value = list[index];
            var type = (element.getAttribute('type') || '').toLowerCase();
            if (type === 'checkbox' || type === 'radio') {
                if (typeof value === 'boolean') {
                    element.checked = value;
                }
            } else if (element.tagName === 'SELECT' && element.multiple) {
                if (Array.isArray(value)) {
                    Array.prototype.forEach.call(element.options, function (option) {
                        option.selected = value.indexOf(option.value) >= 0;
                    });
                }
            } else if (typeof value === 'string') {
                element.value = value;
            }
        });
    }

    function differs(saved, current) {
        return Object.keys(saved).some(function (name) {
            return JSON.stringify(saved[name]) !== JSON.stringify(current[name]);
        });
    }

    function readDraft(key) {
        var raw = storeGet(key);
        if (!raw) {
            return null;
        }
        try {
            var parsed = JSON.parse(raw);
            return parsed && typeof parsed.fields === 'object' && parsed.fields !== null ? parsed.fields : null;
        } catch (e) {
            return null;
        }
    }

    function writeDraft(key, form) {
        storeSet(key, JSON.stringify({fields: snapshot(form)}));
    }

    function draftKey(form) {
        var path;
        try {
            path = new URL(form.getAttribute('action') || window.location.href, window.location.href).pathname;
        } catch (e) {
            path = window.location.pathname;
        }
        return DRAFT_PREFIX + OWNER + ':' + path + ':' + form.getAttribute('data-draft-id');
    }

    function focusFirstField(form) {
        var first = draftFields(form).filter(function (element) {
            return element.getClientRects().length > 0;
        })[0];
        if (first) {
            first.focus();
        }
    }

    // Read while the script runs (deferred: the page is parsed), before the flash re-announce empties it for a moment.
    var RESULT_NOTICE_AT_LOAD = (function () {
        var flash = document.querySelector('[data-js~="flash"]');
        return !!(flash && flash.textContent.trim());
    })();

    // The draft of the form submitted on the previous page: saved -> forget it; shown again with errors -> keep it.
    function settleSubmittedDraft() {
        var pending = storeGet(PENDING_KEY);
        if (!pending) {
            return;
        }
        storeRemove(PENDING_KEY);
        if (RESULT_NOTICE_AT_LOAD && !document.querySelector('[data-js~="error-summary"]')) {
            storeRemove(pending);
        }
    }

    function setUpDraft(form, shownAgain) {
        var key = draftKey(form);
        var id = form.getAttribute('data-draft-id');
        var note = null;
        document.querySelectorAll('[data-js~="draft-note"]').forEach(function (candidate) {
            if (!note && candidate.getAttribute('data-draft-for') === id) {
                note = candidate;
            }
        });
        var saved = readDraft(key);
        var choosing = !!(note && saved && !shownAgain && differs(saved, snapshot(form)));

        function close() {
            choosing = false;
            note.hidden = true;
            focusFirstField(form);
        }

        if (choosing) {
            // Find the buttons before the note is re-announced (that takes its children out for a moment).
            var continueButton = note.querySelector('[data-js~="draft-continue"]');
            var discardButton = note.querySelector('[data-js~="draft-discard"]');
            note.hidden = false;
            reannounce(note);
            if (continueButton) {
                continueButton.addEventListener('click', function () {
                    fill(form, saved);
                    writeDraft(key, form);
                    close();
                });
            }
            if (discardButton) {
                discardButton.addEventListener('click', function () {
                    storeRemove(key);
                    close();
                });
            }
        }

        function remember(event) {
            if (!choosing && draftable(event.target)) {
                writeDraft(key, form);
            }
        }

        form.addEventListener('input', remember);
        form.addEventListener('change', remember);
        form.addEventListener('submit', function () {
            storeSet(PENDING_KEY, key);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        if (!OWNER || !tabStore()) {
            return;
        }
        settleSubmittedDraft();
        // Shown again by the server after a failed save: the error box, or (forms without the box) an invalid field.
        var shownAgain = !!document.querySelector('[data-js~="error-summary"], [data-js~="focus-first-invalid"] [aria-invalid="true"]');
        document.querySelectorAll('form[data-draft-id]').forEach(function (form) {
            setUpDraft(form, shownAgain);
        });
    });

    // Logout forgets every draft of this tab (the next person on this tab must not see them).
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (form instanceof HTMLFormElement && form.matches('[data-js~="logout"]')) {
            forgetAllDrafts();
        }
    });

    // ---------- Keepalive while typing (ADR-38) ----------
    // Typing in a POST form keeps the session alive: at most one GET to the keepalive address every 5 minutes, only
    // after real input. No input, no request, so an unattended page still ends after 30 idle minutes. The answer is
    // not shown (401 after the session ended changes nothing; the tab draft keeps the text).
    var keepaliveMeta = document.querySelector('meta[name="pds-keepalive"]');
    var KEEPALIVE_URL = keepaliveMeta ? keepaliveMeta.getAttribute('content') : null;
    var KEEPALIVE_EVERY_MS = 5 * 60 * 1000;
    var lastKeepalive = Date.now();

    function keepAliveWhileTyping(event) {
        if (!KEEPALIVE_URL || typeof window.fetch !== 'function') {
            return;
        }
        var target = event.target;
        var form = target instanceof Element ? target.closest('form') : null;
        if (!form || (form.getAttribute('method') || 'get').toLowerCase() !== 'post') {
            return;
        }
        var now = Date.now();
        if (now - lastKeepalive < KEEPALIVE_EVERY_MS) {
            return;
        }
        lastKeepalive = now;
        window.fetch(KEEPALIVE_URL, {method: 'GET', credentials: 'same-origin', cache: 'no-store', redirect: 'manual'})
            .catch(function () {
                // Offline or refused: nothing to show.
            });
    }

    document.addEventListener('input', keepAliveWhileTyping, true);
    document.addEventListener('change', keepAliveWhileTyping, true);

    // ---------- Todo list: folded add area and narrow-screen filter panel (ADR-41) ----------
    // Without this script both toggles stay hidden and both areas stay open. Folding is only a view state.

    // The one place that decides whether the add area starts open: the server marks it (data-open="true": the add form
    // came back with errors), or the tab's draft note of the add form is showing (set up above, same load). After a
    // successful add the list page has neither, so the area starts folded and the list comes first.
    function addPanelStartsOpen(panel) {
        if (panel.getAttribute('data-open') === 'true') {
            return true;
        }
        var note = panel.querySelector('[data-js~="draft-note"]');
        return !!(note && !note.hidden);
    }

    function firstFieldIn(container) {
        return Array.prototype.filter.call(container.querySelectorAll('input, select, textarea'), function (element) {
            return (element.getAttribute('type') || '').toLowerCase() !== 'hidden' && !element.disabled
                && element.getClientRects().length > 0;
        })[0];
    }

    function setUpAddPanel() {
        var panel = document.querySelector('[data-js~="add-panel"]');
        var toggle = document.querySelector('[data-js~="add-open"]');
        if (!panel || !toggle) {
            return;
        }
        var closeButton = panel.querySelector('[data-js~="add-close"]');

        // Open: the form shows in place and the bar hides (its words would repeat the form heading). Folded: the
        // bar shows and the form hides.
        function show(open) {
            panel.hidden = !open;
            toggle.hidden = open;
            toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        }

        function close() {
            show(false);
            toggle.focus();
        }

        if (closeButton) {
            closeButton.hidden = false;
            closeButton.addEventListener('click', close);
        }
        show(addPanelStartsOpen(panel));
        toggle.addEventListener('click', function () {
            show(true);
            var first = firstFieldIn(panel);
            if (first) {
                first.focus();
            }
        });
        // Escape inside the form folds it; the typed text stays (and the tab draft keeps it).
        panel.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !event.defaultPrevented) {
                close();
            }
        });
    }

    function setUpFilterPanel() {
        var panel = document.querySelector('[data-js~="filter-panel"]');
        var toggle = document.querySelector('[data-js~="filter-toggle"]');
        if (!panel || !toggle || typeof window.matchMedia !== 'function') {
            return;
        }
        var narrow = window.matchMedia('(max-width: 767px)');

        function show(open) {
            panel.hidden = !open;
            toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        }

        // Narrow: the toggle shows and the panel folds, unless the server keeps it open (data-open: a rejected search
        // whose error must be seen). Wide: no toggle, the panel is always open. Checked again when the width changes.
        function layout() {
            if (narrow.matches) {
                toggle.hidden = false;
                show(panel.getAttribute('data-open') === 'true');
            } else {
                toggle.hidden = true;
                show(true);
            }
        }

        toggle.addEventListener('click', function () {
            if (panel.hidden) {
                show(true);
                var first = firstFieldIn(panel);
                if (first) {
                    first.focus();
                }
            } else {
                show(false);
                toggle.focus();
            }
        });
        if (typeof narrow.addEventListener === 'function') {
            narrow.addEventListener('change', layout);
        }
        layout();
    }

    document.addEventListener('DOMContentLoaded', function () {
        setUpAddPanel();
        setUpFilterPanel();
    });
})();
