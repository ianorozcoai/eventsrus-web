package com.web.eventsrus.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * displayOrder() is the one order every dropdown/list in the app should use
 * (see PlannerController/VendorController) - Anniversary, Birthday, Debut,
 * Wedding always pinned on top (in that order), everything else
 * alphabetical, Other trailing as the catch-all. Corporate Event is
 * deliberately NOT pinned - it sorts alphabetically with the rest.
 */
class EventTypeTest {

    @Test
    void pinsAnniversaryBirthdayDebutWeddingOnTop() {
        List<EventType> order = EventType.displayOrder();

        assertThat(order.subList(0, 4)).containsExactly(
                EventType.ANNIVERSARY, EventType.BIRTHDAY, EventType.DEBUT, EventType.WEDDING);
    }

    @Test
    void corporateEventIsNotPinnedAndSortsAlphabeticallyInstead() {
        List<EventType> order = EventType.displayOrder();
        assertThat(order.subList(0, 4)).doesNotContain(EventType.CORPORATE_EVENT);

        // "Corporate Event" alphabetically belongs right after "Concert".
        int concertIndex = order.indexOf(EventType.CONCERT);
        int corporateEventIndex = order.indexOf(EventType.CORPORATE_EVENT);
        assertThat(corporateEventIndex).isEqualTo(concertIndex + 1);
    }

    @Test
    void othersInBetweenAreAlphabeticalByLabel() {
        List<EventType> order = EventType.displayOrder();
        List<EventType> middle = order.subList(4, order.size() - 1); // excludes the pinned 4 and trailing OTHER

        List<String> labels = middle.stream().map(EventType::getLabel).toList();
        List<String> sortedLabels = labels.stream().sorted().toList();
        assertThat(labels).isEqualTo(sortedLabels);
    }

    @Test
    void otherIsAlwaysLast() {
        List<EventType> order = EventType.displayOrder();
        assertThat(order.get(order.size() - 1)).isEqualTo(EventType.OTHER);
    }

    @Test
    void everyEnumConstantAppearsExactlyOnce() {
        List<EventType> order = EventType.displayOrder();
        assertThat(order).containsExactlyInAnyOrder(EventType.values());
        assertThat(order).doesNotHaveDuplicates();
    }
}
