// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author petekofod
 *
 */
@Entity
@Table(name="logFileIndex")
public class LogFile {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private int id;
	
	@Column(name="mark")
	private String mark;
	
	@Column(name="locoNumber")
	private int locoNumber;
	
	@Column(name="device")
	private String device;
	
	@Column(name="endTime")
	private LocalDateTime endTime;
	
	@Column(name="logFilePath")
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
