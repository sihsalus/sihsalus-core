package org.openmrs.module.appointments.service.impl;

import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.service.AppointmentNumberGenerator;

import jakarta.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DefaultAppointmentNumberGeneratorImpl implements AppointmentNumberGenerator {
    @Override
    public String generateAppointmentNumber(@NotNull Appointment appointment) {
        return new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date());
    }
}
