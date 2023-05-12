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
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import javax.persistence.*;
import java.util.Calendar;

@Table(name = "change_log_entity")
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ChangeLogEntity.ChangeLogEntityBuilder.class)
public class ChangeLogEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long uid;

    @ManyToOne(cascade = CascadeType.ALL)
    private StatusEntity status;

    @Lob
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    private Calendar date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_uid")
    private IssueEntity issue;


}