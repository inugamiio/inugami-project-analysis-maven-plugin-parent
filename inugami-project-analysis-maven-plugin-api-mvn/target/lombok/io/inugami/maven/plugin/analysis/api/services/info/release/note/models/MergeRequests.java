// Generated by delombok at Mon Mar 08 22:38:49 CET 2021
/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.api.services.info.release.note.models;

import lombok.*;

public class MergeRequests {
    String uid;
    String name;
    String url;
    String title;
    String date;


    @java.lang.SuppressWarnings("all")
    public static class MergeRequestsBuilder {
        @java.lang.SuppressWarnings("all")
        private String uid;
        @java.lang.SuppressWarnings("all")
        private String name;
        @java.lang.SuppressWarnings("all")
        private String url;
        @java.lang.SuppressWarnings("all")
        private String title;
        @java.lang.SuppressWarnings("all")
        private String date;

        @java.lang.SuppressWarnings("all")
        MergeRequestsBuilder() {
        }

        @java.lang.SuppressWarnings("all")
        public MergeRequests.MergeRequestsBuilder uid(final String uid) {
            this.uid = uid;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public MergeRequests.MergeRequestsBuilder name(final String name) {
            this.name = name;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public MergeRequests.MergeRequestsBuilder url(final String url) {
            this.url = url;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public MergeRequests.MergeRequestsBuilder title(final String title) {
            this.title = title;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public MergeRequests.MergeRequestsBuilder date(final String date) {
            this.date = date;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public MergeRequests build() {
            return new MergeRequests(this.uid, this.name, this.url, this.title, this.date);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "MergeRequests.MergeRequestsBuilder(uid=" + this.uid + ", name=" + this.name + ", url=" + this.url + ", title=" + this.title + ", date=" + this.date + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static MergeRequests.MergeRequestsBuilder builder() {
        return new MergeRequests.MergeRequestsBuilder();
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MergeRequests)) return false;
        final MergeRequests other = (MergeRequests) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$uid = this.getUid();
        final java.lang.Object other$uid = other.getUid();
        if (this$uid == null ? other$uid != null : !this$uid.equals(other$uid)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$url = this.getUrl();
        final java.lang.Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) return false;
        final java.lang.Object this$title = this.getTitle();
        final java.lang.Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final java.lang.Object this$date = this.getDate();
        final java.lang.Object other$date = other.getDate();
        if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MergeRequests;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $uid = this.getUid();
        result = result * PRIME + ($uid == null ? 43 : $uid.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        final java.lang.Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        return result;
    }

    @java.lang.SuppressWarnings("all")
    public MergeRequests() {
    }

    @java.lang.SuppressWarnings("all")
    public MergeRequests(final String uid, final String name, final String url, final String title, final String date) {
        this.uid = uid;
        this.name = name;
        this.url = url;
        this.title = title;
        this.date = date;
    }

    @java.lang.SuppressWarnings("all")
    public void setUid(final String uid) {
        this.uid = uid;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setUrl(final String url) {
        this.url = url;
    }

    @java.lang.SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    @java.lang.SuppressWarnings("all")
    public void setDate(final String date) {
        this.date = date;
    }

    @java.lang.SuppressWarnings("all")
    public String getUid() {
        return this.uid;
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public String getUrl() {
        return this.url;
    }

    @java.lang.SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    @java.lang.SuppressWarnings("all")
    public String getDate() {
        return this.date;
    }
}