// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.entity;

import java.time.LocalDateTime;

/**
 * Plain data holder for an indexed log-file row.
 *
 * NOTE: persistence is done with plain JDBC in {@code JdbcLogFileRepository}; there is
 * no JPA provider on the classpath. The former {@code jakarta.persistence}
 * {@code @Entity/@Table/@Column} annotations were therefore decorative and have been
 * removed along with the unused dependency. The column mapping now lives solely in the
 * repository's INSERT and {@code scripts/schema.sql}.
 *
 * @author petekofod
 */
public class LogFile {

	private int id;

	private String mark;

	private int locoNumber;

	private String device;

	private LocalDateTime endTime;

	private String logFilePath;
		
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getMark() {
		return mark;
	}
	public void setMark(String mark) {
		this.mark = mark;
	}
	public int getLocoNumber() {
		return locoNumber;
	}
	public void setLocoNumber(int locoNumber) {
		this.locoNumber = locoNumber;
	}
	public String getDevice() {
		return device;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	public LocalDateTime getEndTime() {
		return endTime;
	}
	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}
	public String getLogFilePath() {
		return logFilePath;
	}
	public void setLogFilePath(String logFilePath) {
		this.logFilePath = logFilePath;
	}
	
	public LogFile() {
	}
	
	public LogFile(String mark, int locoNumber, String device, LocalDateTime endTime, String logFilePath) {
		super();
		this.mark = mark;
		this.locoNumber = locoNumber;
		this.device = device;
		this.endTime = endTime;
		this.logFilePath = logFilePath;
	}
	
	@Override
	public String toString() {
		return "LogFile [id=" + id + ", mark=" + mark + ", locoNumber=" + locoNumber + ", device=" + device
				+ ", endTime=" + endTime + ", logFilePath=" + logFilePath + "]";
	}
	
	

}
