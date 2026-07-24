/*
 * This file is part of InteractiveHolograms, licensed under the GNU GPL v3.0 License.
 * Copyright (C) DecentSoftware.eu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.siberanka.interactiveholograms.display;

import com.siberanka.interactiveholograms.api.actions.Action;
import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TextDisplay extends DisplayBase {

    private final List<TextDisplayPage> pages;
    private final Map<UUID, Integer> viewerPages = new ConcurrentHashMap<>();

    public TextDisplay(String name, DecentLocation location, DisplaySettings settings) {
        super(name, location, settings);
        this.pages = new ArrayList<>();
        this.pages.add(new TextDisplayPage());
    }

    @Override
    public DisplayType getType() {
        return DisplayType.TEXT;
    }

    public void addLine(String line) {
        firstPageLines().add(new TextDisplayLine(line, TextDisplayLine.DEFAULT_HEIGHT));
        markContentDirty();
    }

    public void addLine(int index, String line) {
        firstPageLines().add(index, new TextDisplayLine(line, TextDisplayLine.DEFAULT_HEIGHT));
        markContentDirty();
    }

    public void setLine(int index, String line) {
        firstPageLines().get(index).setContent(line);
        markContentDirty();
    }

    public void removeLine(int index) {
        firstPageLines().remove(index);
        markContentDirty();
    }

    public void setLines(List<String> lines) {
        List<TextDisplayLine> converted = lines.stream()
                .map(line -> new TextDisplayLine(line, TextDisplayLine.DEFAULT_HEIGHT))
                .collect(Collectors.toList());
        this.pages.get(0).setLines(converted);
        markContentDirty();
    }

    public void swapLines(int index1, int index2) {
        Collections.swap(firstPageLines(), index1, index2);
        markContentDirty();
    }

    public List<String> getLines() {
        return getLines(null);
    }

    public synchronized void setPages(List<TextDisplayPage> values) {
        pages.clear();
        if (values != null) {
            values.forEach(page -> pages.add(page.copy()));
        }
        if (pages.isEmpty()) {
            pages.add(new TextDisplayPage());
        }
        viewerPages.replaceAll((uuid, index) -> Math.min(index, pages.size() - 1));
        markContentDirty();
        markConfigDirty();
    }

    public synchronized List<TextDisplayPage> getPages() {
        List<TextDisplayPage> copy = new ArrayList<>(pages.size());
        pages.forEach(page -> copy.add(page.copy()));
        return Collections.unmodifiableList(copy);
    }

    public synchronized List<String> getLines(UUID viewerId) {
        return getPage(viewerId).getLines().stream()
                .map(TextDisplayLine::getContent)
                .collect(Collectors.toList());
    }

    public synchronized List<Action> getActions(UUID viewerId, ClickType type) {
        List<Action> result = new ArrayList<>(super.getActions(type));
        result.addAll(getPage(viewerId).getActions(type));
        return result;
    }

    @Override
    public synchronized boolean hasActions() {
        return super.hasActions() || pages.stream().anyMatch(TextDisplayPage::hasActions);
    }

    public int getPageIndex(UUID viewerId) {
        if (viewerId == null) {
            return 0;
        }
        return Math.min(viewerPages.getOrDefault(viewerId, 0), getPageCount() - 1);
    }

    public synchronized int getPageCount() {
        return pages.size();
    }

    public synchronized boolean setViewerPage(UUID viewerId, int pageIndex) {
        if (viewerId == null || pageIndex < 0 || pageIndex >= pages.size()) {
            return false;
        }
        int previous = getPageIndex(viewerId);
        if (previous == pageIndex) {
            return false;
        }
        viewerPages.put(viewerId, pageIndex);
        return true;
    }

    public boolean nextPage(UUID viewerId) {
        return setViewerPage(viewerId, getPageIndex(viewerId) + 1);
    }

    public boolean previousPage(UUID viewerId) {
        return setViewerPage(viewerId, getPageIndex(viewerId) - 1);
    }

    public void retainViewerPages(Set<UUID> onlineViewers) {
        viewerPages.keySet().retainAll(onlineViewers);
    }

    private volatile TextLayoutMetrics cachedMetrics;

    public TextLayoutMetrics getMetrics() {
        long currentRevision = getLayoutRevision();
        TextLayoutMetrics metrics = cachedMetrics;
        if (metrics == null || metrics.getLayoutRevision() != currentRevision) {
            synchronized (this) {
                metrics = cachedMetrics;
                if (metrics == null || metrics.getLayoutRevision() != currentRevision) {
                    double maxHeight = pages.stream().mapToDouble(page -> page.getLines().stream()
                            .mapToDouble(TextDisplayLine::getHeight).sum()).max().orElse(TextDisplayLine.DEFAULT_HEIGHT);
                    int maxLength = pages.stream().flatMap(page -> page.getLines().stream())
                            .mapToInt(line -> TextLayoutScanner.visibleLength(line.getContent())).max().orElse(1);
                    boolean actionsPresent = super.hasActions() || pages.stream().anyMatch(TextDisplayPage::hasActions);
                    boolean placeholdersPresent = pages.stream().flatMap(page -> page.getLines().stream())
                            .anyMatch(line -> line.getContent() != null && line.getContent().contains("%"));
                    boolean animationsPresent = pages.stream().flatMap(page -> page.getLines().stream())
                            .anyMatch(line -> line.getContent() != null && line.getContent().contains("<anim:"));

                    metrics = new TextLayoutMetrics(maxHeight, maxLength, actionsPresent, placeholdersPresent, animationsPresent, currentRevision);
                    cachedMetrics = metrics;
                }
            }
        }
        return metrics;
    }

    public synchronized double getMaximumPageHeight() {
        return getMetrics().getMaximumPageHeight();
    }

    public synchronized int getMaximumVisibleLineLength() {
        return getMetrics().getMaximumVisibleLineLength();
    }

    private synchronized TextDisplayPage getPage(UUID viewerId) {
        return pages.get(getPageIndex(viewerId));
    }

    private List<TextDisplayLine> firstPageLines() {
        return pages.get(0).mutableLines();
    }

    public static int visibleLength(String content) {
        return TextLayoutScanner.visibleLength(content);
    }
}
