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

var swiper = new Swiper(".swiper", {
    direction: 'vertical',
    cssMode: true,
    mousewheel: true,
    keyboard: true,
    pagination: {
        el: ".swiper-pagination",
    },
});

var defaultFontColor = '#3a3a3a';

var themeds = [
    {
        bg: '#222728',
        color: '#909399'
    },
    {
        bg: '#606266',
        color: '#FFFFFF'
    },
    {
        bg: '#909399',
        color: '#FFFFFF'
    },
    {
        bg: '#C0C4CC'
    },
    {
        bg: '#DCDFE6'
    },
]
var page = $('.swiper-slide');
for (let i = 0; i < page.length; i++) {
    var themed = themeds[i % themeds.length];
    $(page[i]).css('background-color', themed?.bg);
    $(page[i]).css('color', themed?.color || defaultFontColor);
}