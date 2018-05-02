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
    public static final String NS = "http://www.w3.org/2006/time#";

    /* Classes */
    public static final IRI DateTimeDescription = createIRI(NS + "DateTimeDescription");
    public static final IRI DateTimeInterval = createIRI(NS + "DateTimeInterval");
    public static final IRI DayOfWeek = createIRI(NS + "DayOfWeek");
    public static final IRI Duration = createIRI(NS + "Duration");
    public static final IRI DurationDescription = createIRI(NS + "DurationDescription");
    public static final IRI GeneralDateTimeDescription = createIRI(NS + "GeneralDateTimeDescription");
    public static final IRI GeneralDurationDescription = createIRI(NS + "GeneralDurationDescription");
    public static final IRI Instant = createIRI(NS + "Instant");
    public static final IRI Interval = createIRI(NS + "Interval");
    public static final IRI MonthOfYear = createIRI(NS + "MonthOfYear");
    public static final IRI ProperInterval = createIRI(NS + "ProperInterval");
    public static final IRI TRS = createIRI(NS + "TRS");
    public static final IRI TemporalDuration = createIRI(NS + "TemporalDuration");
    public static final IRI TemporalEntity = createIRI(NS + "TemporalEntity");
    public static final IRI TemporalPosition = createIRI(NS + "TemporalPosition");
    public static final IRI TemporalUnit = createIRI(NS + "TemporalUnit");
    public static final IRI TimePosition = createIRI(NS + "TimePosition");
    public static final IRI TimeZone = createIRI(NS + "TimeZone");
    public static final IRI Year = createIRI(NS + "Year");

    /* Object Properties */
    public static final IRI after = createIRI(NS + "after");
    public static final IRI before = createIRI(NS + "before");
    public static final IRI dayOfWeek = createIRI(NS + "dayOfWeek");
    public static final IRI hasBeginning = createIRI(NS + "hasBeginning");
    public static final IRI hasDateTimeDescription = createIRI(NS + "hasDateTimeDescription");
    public static final IRI hasDuration = createIRI(NS + "hasDuration");
    public static final IRI hasDurationDescription = createIRI(NS + "hasDurationDescription");
    public static final IRI hasEnd = createIRI(NS + "hasEnd");
    public static final IRI hasTRS = createIRI(NS + "hasTRS");
    public static final IRI hasTemporalDuration = createIRI(NS + "hasTemporalDuration");
    public static final IRI hasTime = createIRI(NS + "hasTime");
    public static final IRI inDateTime = createIRI(NS + "inDateTime");
    public static final IRI inTemporalPosition = createIRI(NS + "inTemporalPosition");
    public static final IRI inTimePosition = createIRI(NS + "inTimePosition");
    public static final IRI inside = createIRI(NS + "inside");
    public static final IRI intervalAfter = createIRI(NS + "intervalAfter");
    public static final IRI intervalBefore = createIRI(NS + "intervalBefore");
    public static final IRI intervalContains = createIRI(NS + "intervalContains");
    public static final IRI intervalDisjoint = createIRI(NS + "intervalDisjoint");
    public static final IRI intervalDuring = createIRI(NS + "intervalDuring");
    public static final IRI intervalEquals = createIRI(NS + "intervalEquals");
    public static final IRI intervalFinishedBy = createIRI(NS + "intervalFinishedBy");
    public static final IRI intervalFinishes = createIRI(NS + "intervalFinishes");
    public static final IRI intervalIn = createIRI(NS + "intervalIn");
    public static final IRI intervalMeets = createIRI(NS + "intervalMeets");
    public static final IRI intervalMetBy = createIRI(NS + "intervalMetBy");
    public static final IRI intervalOverlappedBy = createIRI(NS + "intervalOverlappedBy");
    public static final IRI intervalOverlaps = createIRI(NS + "intervalOverlaps");
    public static final IRI intervalStartedBy = createIRI(NS + "intervalStartedBy");
    public static final IRI intervalStarts = createIRI(NS + "intervalStarts");
    public static final IRI monthOfYear = createIRI(NS + "monthOfYear");
    public static final IRI timeZone = createIRI(NS + "timeZone");
    public static final IRI unitType = createIRI(NS + "unitType");
    public static final IRI xsdDateTime = createIRI(NS + "xsdDateTime");

    /* Datatype Properties */
    public static final IRI day = createIRI(NS + "day");
    public static final IRI dayOfYear = createIRI(NS + "dayOfYear");
    public static final IRI days = createIRI(NS + "days");
    public static final IRI hasXSDDuration = createIRI(NS + "hasXSDDuration");
    public static final IRI hour = createIRI(NS + "hour");
    public static final IRI hours = createIRI(NS + "hours");
    public static final IRI week = createIRI(NS + "week");
    public static final IRI weeks = createIRI(NS + "weeks");
    public static final IRI year = createIRI(NS + "year");
    public static final IRI years = createIRI(NS + "years");
    public static final IRI inXSDDate = createIRI(NS + "inXSDDate");
    public static final IRI inXSDDateTime = createIRI(NS + "inXSDDateTime");
    public static final IRI inXSDDateTimeStamp = createIRI(NS + "inXSDDateTimeStamp");
    public static final IRI inXSDgYear = createIRI(NS + "inXSDgYear");
    public static final IRI inXSDgYearMonth = createIRI(NS + "inXSDgYearMonth");
    public static final IRI minute = createIRI(NS + "minute");
    public static final IRI minutes = createIRI(NS + "minutes");
    public static final IRI month = createIRI(NS + "month");
    public static final IRI months = createIRI(NS + "months");
    public static final IRI nominalPosition = createIRI(NS + "nominalPosition");
    public static final IRI numericDuration = createIRI(NS + "numericDuration");
    public static final IRI numericPosition = createIRI(NS + "numericPosition");
    public static final IRI second = createIRI(NS + "second");
    public static final IRI seconds = createIRI(NS + "seconds");

    /* Datatypes */
    public static final IRI generalDay = createIRI(NS + "generalDay");
    public static final IRI generalMonth = createIRI(NS + "generalMonth");
    public static final IRI generalYear = createIRI(NS + "generalYear");

    /* Individuals */
    public static final IRI unitDay = createIRI(NS + "unitDay");
    public static final IRI unitHour = createIRI(NS + "unitHour");
    public static final IRI unitMinute = createIRI(NS + "unitMinute");
    public static final IRI unitMonth = createIRI(NS + "unitMonth");
    public static final IRI unitSecond = createIRI(NS + "unitSecond");
    public static final IRI unitWeek = createIRI(NS + "unitWeek");
    public static final IRI unitYear = createIRI(NS + "unitYear");
    public static final IRI Friday = createIRI(NS + "Friday");
    public static final IRI Monday = createIRI(NS + "Monday");
    public static final IRI Saturday = createIRI(NS + "Saturday");
    public static final IRI Sunday = createIRI(NS + "Sunday");
    public static final IRI Thursday = createIRI(NS + "Thursday");
    public static final IRI Tuesday = createIRI(NS + "Tuesday");
    public static final IRI Wednesday = createIRI(NS + "Wednesday");
    public static final IRI January = createIRI(NS + "January");

    private Time() {
        // prevent instantiation
    }
}
