/*
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

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the W3C OWL-Time Vocabulary
 *
 * @see <a href="https://www.w3.org/TR/owl-time/">OWL-Time Ontology</a>
 *
 * @author acoburn
 */
public final class Time extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/2006/time#";

    /* Classes */
    public static final IRI DateTimeDescription = createIRI(URI + "DateTimeDescription");
    public static final IRI DateTimeInterval = createIRI(URI + "DateTimeInterval");
    public static final IRI DayOfWeek = createIRI(URI + "DayOfWeek");
    public static final IRI Duration = createIRI(URI + "Duration");
    public static final IRI DurationDescription = createIRI(URI + "DurationDescription");
    public static final IRI GeneralDateTimeDescription = createIRI(URI + "GeneralDateTimeDescription");
    public static final IRI GeneralDurationDescription = createIRI(URI + "GeneralDurationDescription");
    public static final IRI Instant = createIRI(URI + "Instant");
    public static final IRI Interval = createIRI(URI + "Interval");
    public static final IRI MonthOfYear = createIRI(URI + "MonthOfYear");
    public static final IRI ProperInterval = createIRI(URI + "ProperInterval");
    public static final IRI TRS = createIRI(URI + "TRS");
    public static final IRI TemporalDuration = createIRI(URI + "TemporalDuration");
    public static final IRI TemporalEntity = createIRI(URI + "TemporalEntity");
    public static final IRI TemporalPosition = createIRI(URI + "TemporalPosition");
    public static final IRI TemporalUnit = createIRI(URI + "TemporalUnit");
    public static final IRI TimePosition = createIRI(URI + "TimePosition");
    public static final IRI TimeZone = createIRI(URI + "TimeZone");

    /* Object Properties */
    public static final IRI after = createIRI(URI + "after");
    public static final IRI before = createIRI(URI + "before");
    public static final IRI dayOfWeek = createIRI(URI + "dayOfWeek");
    public static final IRI hasBeginning = createIRI(URI + "hasBeginning");
    public static final IRI hasDateTimeDescription = createIRI(URI + "hasDateTimeDescription");
    public static final IRI hasDuration = createIRI(URI + "hasDuration");
    public static final IRI hasDurationDescription = createIRI(URI + "hasDurationDescription");
    public static final IRI hasEnd = createIRI(URI + "hasEnd");
    public static final IRI hasTRS = createIRI(URI + "hasTRS");
    public static final IRI hasTemporalDuration = createIRI(URI + "hasTemporalDuration");
    public static final IRI hasTime = createIRI(URI + "hasTime");
    public static final IRI inDateTime = createIRI(URI + "inDateTime");
    public static final IRI inTemporalPosition = createIRI(URI + "inTemporalPosition");
    public static final IRI inTimePosition = createIRI(URI + "inTimePosition");
    public static final IRI inside = createIRI(URI + "inside");
    public static final IRI intervalAfter = createIRI(URI + "intervalAfter");
    public static final IRI intervalBefore = createIRI(URI + "intervalBefore");
    public static final IRI intervalContains = createIRI(URI + "intervalContains");
    public static final IRI intervalDisjoint = createIRI(URI + "intervalDisjoint");
    public static final IRI intervalDuring = createIRI(URI + "intervalDuring");
    public static final IRI intervalEquals = createIRI(URI + "intervalEquals");
    public static final IRI intervalFinishedBy = createIRI(URI + "intervalFinishedBy");
    public static final IRI intervalFinishes = createIRI(URI + "intervalFinishes");
    public static final IRI intervalIn = createIRI(URI + "intervalIn");
    public static final IRI intervalMeets = createIRI(URI + "intervalMeets");
    public static final IRI intervalMetBy = createIRI(URI + "intervalMetBy");
    public static final IRI intervalOverlappedBy = createIRI(URI + "intervalOverlappedBy");
    public static final IRI intervalOverlaps = createIRI(URI + "intervalOverlaps");
    public static final IRI intervalStartedBy = createIRI(URI + "intervalStartedBy");
    public static final IRI intervalStarts = createIRI(URI + "intervalStarts");
    public static final IRI monthOfYear = createIRI(URI + "monthOfYear");
    public static final IRI timeZone = createIRI(URI + "timeZone");
    public static final IRI unitType = createIRI(URI + "unitType");

    /* Datatype Properties */
    public static final IRI day = createIRI(URI + "day");
    public static final IRI dayOfYear = createIRI(URI + "dayOfYear");
    public static final IRI days = createIRI(URI + "days");
    public static final IRI hasXSDDuration = createIRI(URI + "hasXSDDuration");
    public static final IRI hour = createIRI(URI + "hour");
    public static final IRI hours = createIRI(URI + "hours");
    public static final IRI week = createIRI(URI + "week");
    public static final IRI weeks = createIRI(URI + "weeks");
    public static final IRI year = createIRI(URI + "year");
    public static final IRI years = createIRI(URI + "years");
    public static final IRI inXSDDate = createIRI(URI + "inXSDDate");
    public static final IRI inXSDDateTimeStamp = createIRI(URI + "inXSDDateTimeStamp");
    public static final IRI inXSDgYear = createIRI(URI + "inXSDgYear");
    public static final IRI inXSDgYearMonth = createIRI(URI + "inXSDgYearMonth");
    public static final IRI minute = createIRI(URI + "minute");
    public static final IRI minutes = createIRI(URI + "minutes");
    public static final IRI month = createIRI(URI + "month");
    public static final IRI months = createIRI(URI + "months");
    public static final IRI nominalPosition = createIRI(URI + "nominalPosition");
    public static final IRI numericDuration = createIRI(URI + "numericDuration");
    public static final IRI numericPosition = createIRI(URI + "numericPosition");
    public static final IRI second = createIRI(URI + "second");
    public static final IRI seconds = createIRI(URI + "seconds");

    /* Datatypes */
    public static final IRI generalDay = createIRI(URI + "generalDay");
    public static final IRI generalMonth = createIRI(URI + "generalMonth");
    public static final IRI generalYear = createIRI(URI + "generalYear");

    /* Individuals */
    public static final IRI unitDay = createIRI(URI + "unitDay");
    public static final IRI unitHour = createIRI(URI + "unitHour");
    public static final IRI unitMinute = createIRI(URI + "unitMinute");
    public static final IRI unitMonth = createIRI(URI + "unitMonth");
    public static final IRI unitSecond = createIRI(URI + "unitSecond");
    public static final IRI unitWeek = createIRI(URI + "unitWeek");
    public static final IRI unitYear = createIRI(URI + "unitYear");
    public static final IRI Friday = createIRI(URI + "Friday");
    public static final IRI Monday = createIRI(URI + "Monday");
    public static final IRI Saturday = createIRI(URI + "Saturday");
    public static final IRI Sunday = createIRI(URI + "Sunday");
    public static final IRI Thursday = createIRI(URI + "Thursday");
    public static final IRI Tuesday = createIRI(URI + "Tuesday");
    public static final IRI Wednesday = createIRI(URI + "Wednesday");

    private Time() {
        super();
    }
}
