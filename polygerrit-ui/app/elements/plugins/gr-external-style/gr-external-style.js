/**
 * @license
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function() {
  'use strict';

  Polymer({
    is: 'gr-external-style',
    _legacyUndefinedCheck: true,

    properties: {
      name: String,
      _urlsImported: {
        type: Array,
        value() { return []; },
      },
      _stylesApplied: {
        type: Array,
        value() { return []; },
      },
    },

    /**
     * @suppress {checkTypes}
     */
    _import(url) {
      if (this._urlsImported.includes(url)) { return Promise.resolve(); }
      this._urlsImported.push(url);
      return new Promise((resolve, reject) => {
        (this.importHref || Polymer.importHref)(url, resolve, reject);
      });
    },

    _applyStyle(name) {
      if (this._stylesApplied.includes(name)) { return; }
      this._stylesApplied.push(name);
      // Hybrid custom-style syntax:
      // https://polymer-library.polymer-project.org/2.0/docs/devguide/style-shadow-dom
      const s = document.createElement('style', 'custom-style');
      s.setAttribute('include', name);
      const cs = document.createElement('custom-style');
      cs.appendChild(s);
      // When using Shadow DOM <custom-style> must be added to the <body>.
      // Within <gr-external-style> itself the styles would have no effect.
      const topEl = document.getElementsByTagName('body')[0];
      topEl.insertBefore(cs, topEl.firstChild);
    },

    _importAndApply() {
      Promise.all(Gerrit._endpoints.getPlugins(this.name).map(
          pluginUrl => this._import(pluginUrl))
      ).then(() => {
        const moduleNames = Gerrit._endpoints.getModules(this.name);
        for (const name of moduleNames) {
          this._applyStyle(name);
        }
      });
    },

    attached() {
      this._importAndApply();
    },

    ready() {
      Gerrit.awaitPluginsLoaded().then(() => this._importAndApply());
    },
  });
})();
