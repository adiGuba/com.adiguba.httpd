package com.adiguba.httpd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class MultiMap {
	
	private final Map<String,String[]> values = new HashMap<>();
	
	
	public void setValue(String name, String value) {
		this.values.put(name, new String[]{value});
	}
	
	public void addValue(String name, String value) {
		String[] values = this.values.get(name);
		if (values!=null) {
			int n = values.length;
			values = Arrays.copyOf(values, n+1);
			values[n] = value;
		} else {
			values = new String[] {value};
		}
		this.values.put(name, values);
	}
	
	public String getValue(String name) {
		String[] values = getValues(name);
		if (values!=null) {
			return values[0];
		}
		return null;
	}
	
	public String[] getValues(String name) {
		return this.values.get(name);
	}

	Iterable<Map.Entry<String, String[]>> values() {
		return this.values.entrySet();
	}
	
	Iterable<String> keys() {
		return this.values.keySet();
	}
	
	@Override
	public String toString() {
		return this.values.entrySet().stream()
				.map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
				.collect(Collectors.joining(", "));
	}
}
