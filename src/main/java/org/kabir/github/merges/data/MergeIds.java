/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.kabir.github.merges.data;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@RequestScoped
@Named
public class MergeIds {

    private String ids;
    private List<PullRequestInfo> infos;
    private MergeState state = MergeState.NEW;
    private String name;

    public void setPullRequestIds(String ids){
        this.ids = ids;
    }

    public String getPullRequestIds(){
        return ids;
    }

    public List<PullRequestInfo> getPullRequestInfos() {
        return infos;
    }

    public void setPullRequestInfos(List<PullRequestInfo> infos) {
        this.infos = infos;
    }

    public static class PullRequestInfo {
        final int id;
        final String url;
        final String title;
        final String state;

        public PullRequestInfo(int id, String url, String title, String state) {
            this.id = id;
            this.url = url;
            this.title = title;
            this.state = state;
        }

        public String getUrl() {
            return url;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getState() {
            return state;
        }
    }

    public void setState(MergeState state) {
        this.state = state;
    }

    public String getStateString() {
        return state.getStateString();
    }

    public boolean isOpen() {
        return state.isOpen();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
