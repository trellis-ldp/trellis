/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
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
package org.trellisldp.vocabulary;

import static org.trellisldp.vocabulary.VocabUtils.createIRI;

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the W3C OWL-Time Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/owl-time/">OWL-Time Ontology</a>
 *
 * @author acoburn
 */
public final class Time {

    /* Namespace */
    private static final String URI = "http://www.w3.org/2006/time#";

    /* Classes */
    public static final IRI DateTimeDescription = createIRI(getNamespace() + "DateTimeDescription");
    public static final IRI DateTimeInterval = createIRI(getNamespace() + "DateTimeInterval");
    public static final IRI DayOfWeek = createIRI(getNamespace() + "DayOfWeek");
    public static final IRI Duration = createIRI(getNamespace() + "Duration");
    public static final IRI DurationDescription = createIRI(getNamespace() + "DurationDescription");
    public static final IRI GeneralDateTimeDescription = createIRI(getNamespace() + "GeneralDateTimeDescription");
    public static final IRI GeneralDurationDescription = createIRI(getNamespace() + "GeneralDurationDescription");
    public static final IRI Instant = createIRI(getNamespace() + "Instant");
    public static final IRI Interval = createIRI(getNamespace() + "Interval");
    public static final IRI MonthOfYear = createIRI(getNamespace() + "MonthOfYear");
    public static final IRI ProperInterval = createIRI(getNamespace() + "ProperInterval");
    public static final IRI TRS = createIRI(getNamespace() + "TRS");
    public static final IRI TemporalDuration = createIRI(getNamespace() + "TemporalDuration");
    public static final IRI TemporalEntity = createIRI(getNamespace() + "TemporalEntity");
    public static final IRI TemporalPosition = createIRI(getNamespace() + "TemporalPosition");
    public static final IRI TemporalUnit = createIRI(getNamespace() + "TemporalUnit");
    public static final IRI TimePosition = createIRI(getNamespace() + "TimePosition");
    public static final IRI TimeZone = createIRI(getNamespace() + "TimeZone");

    /* Object Properties */
    public static final IRI after = createIRI(getNamespace() + "after");
    public static final IRI before = createIRI(getNamespace() + "before");
    public static final IRI dayOfWeek = createIRI(getNamespace() + "dayOfWeek");
    public static final IRI hasBeginning = createIRI(getNamespace() + "hasBeginning");
    public static final IRI hasDateTimeDescription = createIRI(getNamespace() + "hasDateTimeDescription");
    public static final IRI hasDuration = createIRI(getNamespace() + "hasDuration");
    public static final IRI hasDurationDescription = createIRI(getNamespace() + "hasDurationDescription");
    public static final IRI hasEnd = createIRI(getNamespace() + "hasEnd");
    public static final IRI hasTRS = createIRI(getNamespace() + "hasTRS");
    public static final IRI hasTemporalDuration = createIRI(getNamespace() + "hasTemporalDuration");
    public static final IRI hasTime = createIRI(getNamespace() + "hasTime");
    public static final IRI inDateTime = createIRI(getNamespace() + "inDateTime");
    public static final IRI inTemporalPosition = createIRI(getNamespace() + "inTemporalPosition");
    public static final IRI inTimePosition = createIRI(getNamespace() + "inTimePosition");
    public static final IRI inside = createIRI(getNamespace() + "inside");
    public static final IRI intervalAfter = createIRI(getNamespace() + "intervalAfter");
    public static final IRI intervalBefore = createIRI(getNamespace() + "intervalBefore");
    public static final IRI intervalContains = createIRI(getNamespace() + "intervalContains");
    public static final IRI intervalDisjoint = createIRI(getNamespace() + "intervalDisjoint");
    public static final IRI intervalDuring = createIRI(getNamespace() + "intervalDuring");
    public static final IRI intervalEquals = createIRI(getNamespace() + "intervalEquals");
    public static final IRI intervalFinishedBy = createIRI(getNamespace() + "intervalFinishedBy");
    public static final IRI intervalFinishes = createIRI(getNamespace() + "intervalFinishes");
    public static final IRI intervalIn = createIRI(getNamespace() + "intervalIn");
    public static final IRI intervalMeets = createIRI(getNamespace() + "intervalMeets");
    public static final IRI intervalMetBy = createIRI(getNamespace() + "intervalMetBy");
    public static final IRI intervalOverlappedBy = createIRI(getNamespace() + "intervalOverlappedBy");
    public static final IRI intervalOverlaps = createIRI(getNamespace() + "intervalOverlaps");
    public static final IRI intervalStartedBy = createIRI(getNamespace() + "intervalStartedBy");
    public static final IRI intervalStarts = createIRI(getNamespace() + "intervalStarts");
    public static final IRI monthOfYear = createIRI(getNamespace() + "monthOfYear");
    public static final IRI timeZone = createIRI(getNamespace() + "timeZone");
    public static final IRI unitType = createIRI(getNamespace() + "unitType");

    /* Datatype Properties */
    public static final IRI day = createIRI(getNamespace() + "day");
    public static final IRI dayOfYear = createIRI(getNamespace() + "dayOfYear");
    public static final IRI days = createIRI(getNamespace() + "days");
    public static final IRI hasXSDDuration = createIRI(getNamespace() + "hasXSDDuration");
    public static final IRI hour = createIRI(getNamespace() + "hour");
    public static final IRI hours = createIRI(getNamespace() + "hours");
    public static final IRI week = createIRI(getNamespace() + "week");
    public static final IRI weeks = createIRI(getNamespace() + "weeks");
    public static final IRI year = createIRI(getNamespace() + "year");
    public static final IRI years = createIRI(getNamespace() + "years");
    public static final IRI inXSDDate = createIRI(getNamespace() + "inXSDDate");
    public static final IRI inXSDDateTimeStamp = createIRI(getNamespace() + "inXSDDateTimeStamp");
    public static final IRI inXSDgYear = createIRI(getNamespace() + "inXSDgYear");
    public static final IRI inXSDgYearMonth = createIRI(getNamespace() + "inXSDgYearMonth");
    public static final IRI minute = createIRI(getNamespace() + "minute");
    public static final IRI minutes = createIRI(getNamespace() + "minutes");
    public static final IRI month = createIRI(getNamespace() + "month");
    public static final IRI months = createIRI(getNamespace() + "months");
    public static final IRI nominalPosition = createIRI(getNamespace() + "nominalPosition");
    public static final IRI numericDuration = createIRI(getNamespace() + "numericDuration");
    public static final IRI numericPosition = createIRI(getNamespace() + "numericPosition");
    public static final IRI second = createIRI(getNamespace() + "second");
    public static final IRI seconds = createIRI(getNamespace() + "seconds");

    /* Datatypes */
    public static final IRI generalDay = createIRI(getNamespace() + "generalDay");
    public static final IRI generalMonth = createIRI(getNamespace() + "generalMonth");
    public static final IRI generalYear = createIRI(getNamespace() + "generalYear");

    /* Individuals */
    public static final IRI unitDay = createIRI(getNamespace() + "unitDay");
    public static final IRI unitHour = createIRI(getNamespace() + "unitHour");
    public static final IRI unitMinute = createIRI(getNamespace() + "unitMinute");
    public static final IRI unitMonth = createIRI(getNamespace() + "unitMonth");
    public static final IRI unitSecond = createIRI(getNamespace() + "unitSecond");
    public static final IRI unitWeek = createIRI(getNamespace() + "unitWeek");
    public static final IRI unitYear = createIRI(getNamespace() + "unitYear");
    public static final IRI Friday = createIRI(getNamespace() + "Friday");
    public static final IRI Monday = createIRI(getNamespace() + "Monday");
    public static final IRI Saturday = createIRI(getNamespace() + "Saturday");
    public static final IRI Sunday = createIRI(getNamespace() + "Sunday");
    public static final IRI Thursday = createIRI(getNamespace() + "Thursday");
    public static final IRI Tuesday = createIRI(getNamespace() + "Tuesday");
    public static final IRI Wednesday = createIRI(getNamespace() + "Wednesday");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private Time() {
        // prevent instantiation
    }
}
