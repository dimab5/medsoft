package com.his.models;

import com.his.models.enums.VisitStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visit {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "patient_id", nullable = false)
	private PatientEntity patient;

	@Column(nullable = false)
	private LocalDateTime visitTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private VisitStatus status;
}
