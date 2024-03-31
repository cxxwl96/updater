/*
 * Copyright (c) 2021-2024, cxxwl96.com (cxxwl96@sina.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var defaultFontColor = '#3a3a3a';

var themed = [
    {
        bg: '#222728',
        color: '#9e9e9e'
    },
    {
        bg: '#5D5E69',
        color: '#ffffff'
    },
    {
        bg: '#CCCCFF'
    },
    {
        bg: '#CCFFFF'
    },
    {
        bg: '#99CCFF'
    },
]
var page = $('.page');
for (let i = 0; i < page.length; i++) {
    $(page[i]).css('background-color', themed[i].bg);
    $(page[i]).css('color', themed[i].color || defaultFontColor);
}