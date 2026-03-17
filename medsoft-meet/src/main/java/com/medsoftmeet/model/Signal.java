package com.medsoftmeet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Signal {
	private String type;
	private String from;
	private String to;
	private Object data;
}