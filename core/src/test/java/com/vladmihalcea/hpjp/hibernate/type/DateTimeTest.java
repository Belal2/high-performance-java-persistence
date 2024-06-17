package com.vladmihalcea.hpjp.hibernate.type;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DateTimeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            LocalDateEvent.class,
            ZonedDateTimeEvent.class,
            OffsetDateTimeEvent.class,
            TimestampEvent.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.JDBC_TIME_ZONE, "UTC");
    }

    @Test
    public void testLocalDateEvent() {
        doInJPA(entityManager -> {
            LocalDateEvent event = new LocalDateEvent();
            event.id = 1L;
            event.startDate = LocalDate.of(1, 1, 1);
            entityManager.persist(event);
        });

        doInJPA(entityManager -> {
            LocalDateEvent event = entityManager.find(LocalDateEvent.class, 1L);
            try {
                assertEquals(LocalDate.of(1, 1, 1), event.startDate);
            } catch (Throwable e) {
                LOGGER.error("Failed", e);
            }
        });
    }

    @Test
    public void testOffsetDateTimeEvent() {
        doInJPA(entityManager -> {
            OffsetDateTimeEvent event = new OffsetDateTimeEvent();
            event.id = 1L;
            event.startDate = OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            entityManager.persist(event);
        });

        doInJPA(entityManager -> {
            OffsetDateTimeEvent event = entityManager.find(OffsetDateTimeEvent.class, 1L);
            try {
                assertEquals(OffsetDateTime.of(2017, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), event.startDate);
            } catch (Throwable e) {
                LOGGER.error("Failed", e);
            }
        });
    }

    @Test
    public void testZonedDateTimeEvent() {
        doInJPA(entityManager -> {
            ZonedDateTimeEvent event = new ZonedDateTimeEvent();
            event.id = 1L;
            event.startDate = ZonedDateTime.of(
                2017, 6, 24, 15, 45, 23, 0, ZoneOffset.systemDefault());
            entityManager.persist(event);
        });

        doInJPA(entityManager -> {
            ZonedDateTimeEvent event = entityManager.find(ZonedDateTimeEvent.class, 1L);
            try {
                assertEquals(ZonedDateTime.of(
                2017, 6, 24, 15, 45, 23, 0, ZoneOffset.systemDefault()), event.startDate);
            } catch (Throwable e) {
                LOGGER.error("Failed", e);
            }
        });
    }

    @Test
    public void testTruncEvent() {
        doInJPA(entityManager -> {
            TimestampEvent event = new TimestampEvent();
            event.id = 1L;
            event.createdOn = new Date();
            entityManager.persist(event);
        });

        doInJPA(entityManager -> {
            List<TimestampEvent> events = entityManager.createQuery("""
                    select e
                    from TimestampEvent e
                    where cast(e.createdOn as date) >= :createdOn
                    order by e.createdOn asc
                    """)
                .setParameter(
                    "createdOn",
                    Timestamp.from(
                        LocalDateTime.of(
                            LocalDate.now(),
                            LocalTime.MIDNIGHT
                        ).toInstant(ZoneOffset.UTC)
                    ), TemporalType.DATE
                )
                .getResultList();
            assertEquals(1, events.size());
        });
        doInJPA(entityManager -> {
            LocalDateTime dt = LocalDateTime.now();
            ZonedDateTime zdt = dt.atZone(ZoneOffset.systemDefault());
            ZoneOffset offset = zdt.getOffset();

            List<TimestampEvent> events = entityManager.createQuery("""
                select e
                from TimestampEvent e
                where function('date_trunc', 'hour', e.createdOn) >= :createdOn
                order by e.createdOn asc
                """)
                .setParameter(
                    "createdOn",
                    Timestamp.from(
                        LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
                            .minusSeconds(offset.getTotalSeconds())
                            .toInstant(ZoneOffset.UTC)
                    ),
                    TemporalType.DATE
                )
                .getResultList();
            assertEquals(1, events.size());
        });
    }

    @Entity(name = "LocalDateEvent")
    public static class LocalDateEvent {

        @Id
        private Long id;

        @NotNull
        @Column(name = "START_DATE", nullable = false)
        private LocalDate startDate;
    }

    @Entity(name = "OffsetDateTimeEvent")
    public static class OffsetDateTimeEvent {

        @Id
        private Long id;

        @NotNull
        @Column(name = "START_DATE", nullable = false)
        private OffsetDateTime startDate;
    }

    @Entity(name = "ZonedDateTimeEvent")
    public static class ZonedDateTimeEvent {

        @Id
        private Long id;

        @NotNull
        @Column(name = "START_DATE", nullable = false)
        private ZonedDateTime startDate;
    }

    @Entity(name = "TimestampEvent")
    public static class TimestampEvent {

        @Id
        private Long id;

        @Column(name = "START_DATE")
        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn;
    }
}
