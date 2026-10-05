package ui;

import dao.DiscountEventDAO;
import model.Vehicle;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.function.BiFunction;

/**
 * Reserve window: vehicle photo, rates, two date pickers and a live price estimate (see ReserveForm).
 
 */
public class ReserveDialog extends JDialog {

    private final ReserveForm form;

    /** Shows the dialog; returns {start, end} when the customer confirmed, or null when cancelled. */
    public static LocalDate[] ask(Window owner, Vehicle v, DiscountEventDAO discountDAO,
                                  BiFunction<LocalDate, LocalDate, String> validator) {
        ReserveDialog d = new ReserveDialog(owner, v, discountDAO, validator);
        d.setVisible(true);
        return d.form.result();
    }

    private ReserveDialog(Window owner, Vehicle v, DiscountEventDAO discountDAO,
                          BiFunction<LocalDate, LocalDate, String> validator) {
        super(owner, "Reserve " + v.getName(), ModalityType.APPLICATION_MODAL);
        form = new ReserveForm(v, discountDAO, validator, this::dispose);
        setContentPane(form);
        getRootPane().setDefaultButton(form.confirmButton());
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}
