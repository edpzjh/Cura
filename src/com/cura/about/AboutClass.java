/*
 Copyright© 2010, 2011 Ahmad Balaa, Oday Maleh

 This file is part of Cura.

	Cura is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cura is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Cura.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cura.about;

/*
 * Description: This class is used to create an object of and feed it two properties (the title and the subtitle)
 * (i.e. "Author", "TTCO Development Team")
 */

public class AboutClass {
 private String title;
 private String subtitle;

 public AboutClass(String title, String subtitle) {
  this.title = title;
  this.subtitle = subtitle;
 }

 public String getTitle() {
  return title;
 }

 public void setTitle(String title) {
  this.title = title;
 }

 public String getSubtitle() {
  return subtitle;
 }

 public void setSubtitle(String subtitle) {
  this.subtitle = subtitle;
 }
}
