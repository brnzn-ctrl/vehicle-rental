package ui;



import util.Validator;



import model.Staff;

import util.AccessControl;

import util.ErrorHandler;







import util.ImageUtil;

import dao.VehicleCategoryDAO;

import dao.VehicleDAO;

import model.Vehicle;

import model.VehicleCategory;

import javax.imageio.ImageIO;

import javax.swing.*;

import javax.swing.table.DefaultTableModel;

import java.awt.*;

import java.awt.image.BufferedImage;

import java.io.File;

import java.math.BigDecimal;

import java.util.List;



/** Admin/Employee screen: list vehicles, add/edit/delete, upload+crop+resize photo. */

public class VehiclePanel extends JPanel {



    private static final String IMAGE_FOLDER = "VehicleImages";

    private static final int THUMB = 150;        // size of the small preview box on this screen

    private static final int PHOTO_MAX = 800;     // longest side of the saved photo (customer cards show it large)



    private final VehicleDAO vehicleDAO = new VehicleDAO();

    private final VehicleCategoryDAO categoryDAO = new VehicleCategoryDAO();



    private final DefaultTableModel tableModel =

        new DefaultTableModel(new Object[]{"ID", "Plate", "Name", "Category", "Rate/day", "Status"}, 0) {

            @Override public boolean isCellEditable(int r, int c) { return false; }

        };

    private final JTable table = new JTable(tableModel);

    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected



    private final JTextField txtPlate = new JTextField(15);

    private final JTextField txtName = new JTextField(15);

    private final JComboBox<VehicleCategory> cboCategory = new JComboBox<>();

    private final JTextField txtRate = new JTextField(10);

    private final JComboBox<String> cboStatus = new JComboBox<>(new String[]{"available", "rented", "maintenance", "inactive"});

    private final JLabel lblPreview = new JLabel();



    private BufferedImage stagedImage;      // photo waiting to be saved on Add/Update

    private String existingImageFilename;   // photo already on disk when editing

    private Vehicle selected;               // currently selected row, null = "adding new"



    private final Staff loggedInStaff;   // used for the admin-only Delete



    /** Without a logged-in staff member nobody counts as admin, so Delete stays locked. Use VehiclePanel(Staff). */

    public VehiclePanel() { this(null); }



    public VehiclePanel(Staff loggedInStaff) {

        this.loggedInStaff = loggedInStaff;

        setLayout(new BorderLayout(10, 10));

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));



        add(buildForm(), BorderLayout.NORTH);

        FormSupport.onDoubleClick(table, this::onRowSelected);   // MouseListener: double-click a row to load it into the form

        add(SearchBar.wrap(table, this::runSearch), BorderLayout.CENTER);



        table.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) onRowSelected();

        });



        addComponentListener(new java.awt.event.ComponentAdapter() {

            @Override public void componentShown(java.awt.event.ComponentEvent e) { VehicleCategory keep = (VehicleCategory) cboCategory.getSelectedItem(); loadCategories(); if (keep != null) selectComboByCategoryId(keep.getId()); refreshTable(); }

        });

        loadCategories();

        refreshTable();

    }



    private JPanel buildForm() {

        JPanel form = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();

        gc.insets = new Insets(4, 4, 4, 4);

        gc.anchor = GridBagConstraints.WEST;



        int row = 0;

        addField(form, gc, row++, "ID:", txtId);

        addField(form, gc, row++, "Plate Number:", txtPlate);

        addField(form, gc, row++, "Vehicle Name:", txtName);

        addField(form, gc, row++, "Category:", cboCategory);

        addField(form, gc, row++, "Daily Rate:", txtRate);

        addField(form, gc, row++, "Status:", cboStatus);



        lblPreview.setPreferredSize(new Dimension(THUMB, THUMB));

        lblPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        lblPreview.setHorizontalAlignment(SwingConstants.CENTER);

        lblPreview.setText("No Photo");

        gc.gridx = 2; gc.gridy = 0; gc.gridheight = 5;

        form.add(lblPreview, gc);

        gc.gridheight = 1;



        JButton btnUpload = new JButton("Upload Photo");

        btnUpload.addActionListener(e -> uploadPhoto());

        JButton btnCrop = new JButton("Crop Again");

        btnCrop.addActionListener(e -> cropStagedPhoto());

        JButton btnRemovePhoto = new JButton("Remove Photo");

        btnRemovePhoto.addActionListener(e -> removePhoto());



        gc.gridx = 3; gc.gridy = 0; form.add(btnUpload, gc);

        gc.gridy = 1; form.add(btnCrop, gc);

        gc.gridy = 2; form.add(btnRemovePhoto, gc);



        JButton btnAdd = new JButton("Save");

        JButton btnUpdate = new JButton("Update");

        JButton btnDelete = new JButton("Delete");

        JButton btnClear = new JButton("Clear");

        JButton btnRefresh = new JButton("Refresh");

        btnRefresh.addActionListener(e -> ErrorHandler.run(this, () -> { VehicleCategory keep = (VehicleCategory) cboCategory.getSelectedItem(); loadCategories(); if (keep != null) selectComboByCategoryId(keep.getId()); refreshTable(); }));

        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::addVehicle));

        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateVehicle));

        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteVehicle));

        btnClear.addActionListener(e -> clearForm());



        JPanel buttons = new JPanel();

        buttons.add(btnAdd);

        buttons.add(btnUpdate);

        buttons.add(btnDelete);

        buttons.add(btnClear);

        buttons.add(btnRefresh);

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 4;

        form.add(buttons, gc);



        return form;

    }



    private void addField(JPanel form, GridBagConstraints gc, int row, String label, JComponent field) {

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;

        form.add(new JLabel(label), gc);

        gc.gridx = 1;

        form.add(field, gc);

    }



    private void loadCategories() {

        cboCategory.removeAllItems();

        for (VehicleCategory c : categoryDAO.findAll()) cboCategory.addItem(c);

    }



    /** Keyword currently typed in the search box ("" = show all). */

    private String keyword = "";



    private void runSearch(String text) {

        keyword = text;

        refreshTable();

    }



    private void refreshTable() {

        tableModel.setRowCount(0);

        for (Vehicle v : vehicleDAO.search(keyword)) {

            tableModel.addRow(new Object[]{v.getId(), v.getPlateNumber(), v.getName(), v.getCategoryName(), v.getDailyRate(), v.getStatus()});

        }

    }



    private void onRowSelected() {

        int row = table.getSelectedRow();

        if (row < 0) return;

        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);

        selected = vehicleDAO.findById(id);

        if (selected == null) {

            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");

            refreshTable();

            return;

        }

        FormSupport.showId(txtId, selected.getId());



        txtPlate.setText(selected.getPlateNumber());

        txtName.setText(selected.getName());

        txtRate.setText(selected.getDailyRate().toString());

        cboStatus.setSelectedItem(selected.getStatus());

        selectComboByCategoryId(selected.getCategoryId());



        stagedImage = null;

        existingImageFilename = selected.getImageFilename();

        showPreview(loadExistingThumb());

    }



    private void selectComboByCategoryId(int categoryId) {

        for (int i = 0; i < cboCategory.getItemCount(); i++) {

            if (cboCategory.getItemAt(i).getId() == categoryId) { cboCategory.setSelectedIndex(i); return; }

        }

    }



    private BufferedImage loadExistingThumb() {

        if (existingImageFilename == null) return null;

        try {

            return ImageUtil.loadFromDisk(IMAGE_FOLDER, existingImageFilename);

        } catch (Exception e) {

            return null;

        }

    }



    // ---------- photo handling ----------



    private void uploadPhoto() {

        JFileChooser chooser = new JFileChooser();

        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "bmp"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;



        try {

            BufferedImage original = ImageIO.read(chooser.getSelectedFile());

            CropDialog dialog = new CropDialog(SwingUtilities.getWindowAncestor(this), original);

            dialog.setVisible(true);

            BufferedImage cropped = (dialog.result != null) ? dialog.result : original;



            stagedImage = ImageUtil.fitWithin(cropped, PHOTO_MAX);   // keep it sharp for the customer cards

            showPreview(stagedImage);

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(this, "Could not load image: " + ex.getMessage());

        }

    }



    private void cropStagedPhoto() {

        BufferedImage base = (stagedImage != null) ? stagedImage : loadExistingThumb();

        if (base == null) { JOptionPane.showMessageDialog(this, "Upload a photo first."); return; }

        CropDialog dialog = new CropDialog(SwingUtilities.getWindowAncestor(this), base);

        dialog.setVisible(true);

        if (dialog.result != null) {

            stagedImage = ImageUtil.fitWithin(dialog.result, PHOTO_MAX);

            showPreview(stagedImage);

        }

    }



    private void removePhoto() {

        stagedImage = null;

        existingImageFilename = null;

        showPreview(null);

    }



    private void showPreview(BufferedImage img) {

        if (img == null) { lblPreview.setIcon(null); lblPreview.setText("No Photo"); return; }

        lblPreview.setText("");

        lblPreview.setIcon(new ImageIcon(ImageUtil.fitWithin(img, THUMB)));   // preview only; the saved file stays large

    }



    // ---------- CRUD ----------



    private void addVehicle() {

        Vehicle v = readForm(new Vehicle());

        if (v == null) return;

        vehicleDAO.insert(v);

        if (stagedImage != null) saveStagedImage(v.getId(), v);

        JOptionPane.showMessageDialog(this, "Vehicle added.");

        clearForm();

        refreshTable();

    }



    private void updateVehicle() {

        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a vehicle first."); return; }

        Vehicle v = readForm(selected);

        if (v == null) return;

        v.setImageFilename(existingImageFilename); // keep unless a new photo was staged below

        if (stagedImage != null) saveStagedImage(v.getId(), v);

        vehicleDAO.update(v);

        JOptionPane.showMessageDialog(this, "Vehicle updated.");

        clearForm();

        refreshTable();

    }



    private void deleteVehicle() {

        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete vehicles")) return;

        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a vehicle first."); return; }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?\n" + selected.getPlateNumber(), "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        vehicleDAO.delete(selected.getId());

        JOptionPane.showMessageDialog(this, "Vehicle deleted.");

        clearForm();

        refreshTable();

    }



    private void saveStagedImage(int vehicleId, Vehicle v) {

        try {

            String filename = vehicleId + ".png";

            ImageUtil.saveToDisk(stagedImage, IMAGE_FOLDER, filename);

            v.setImageFilename(filename);

            vehicleDAO.update(v); // write the filename now that we know the id

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(this, "Could not save photo: " + ex.getMessage());

        }

    }



    /** Validates the form and copies values into v. Returns null (and shows a message) if invalid. */

    private Vehicle readForm(Vehicle v) {

        if (txtPlate.getText().isBlank() || txtName.getText().isBlank()) {

            JOptionPane.showMessageDialog(this, "Plate number and name are required.");

            return null;

        }

        if (vehicleDAO.plateExists(txtPlate.getText(), v.getId())) {

            JOptionPane.showMessageDialog(this, "That plate number is already registered to another vehicle.");

            return null;

        }

        VehicleCategory cat = (VehicleCategory) cboCategory.getSelectedItem();

        if (cat == null) { JOptionPane.showMessageDialog(this, "Select a category."); return null; }



        BigDecimal rate = Validator.parsePositive(txtRate.getText());

        if (rate == null) {

            JOptionPane.showMessageDialog(this, "Daily rate must be a number greater than 0, e.g. 1500.00");

            return null;

        }



        // "rented" must always match the rentals table: it is set by Rentals > Check Out and cleared by Return & Pay,

        // never by hand (otherwise one vehicle could be handed to two customers).

        String newStatus = (String) cboStatus.getSelectedItem();

        boolean outOnRental = v.getId() != 0 && vehicleDAO.hasOngoingRental(v.getId());

        if (outOnRental && !"rented".equals(newStatus)) {

            JOptionPane.showMessageDialog(this, "This vehicle is out on an ongoing rental.\nUse Rentals > Return & Pay before changing its status.");

            return null;

        }

        if (!outOnRental && "rented".equals(newStatus) && !"rented".equals(v.getStatus())) {

            JOptionPane.showMessageDialog(this, "A vehicle becomes \"rented\" only through Rentals > Check Out.");

            return null;

        }



        v.setPlateNumber(txtPlate.getText().trim());

        v.setName(txtName.getText().trim());

        v.setCategoryId(cat.getId());

        v.setDailyRate(rate);

        v.setStatus((String) cboStatus.getSelectedItem());

        return v;

    }



    private void clearForm() {

        selected = null;

        txtId.setText("");

        stagedImage = null;

        existingImageFilename = null;

        txtPlate.setText("");

        txtName.setText("");

        txtRate.setText("");

        cboStatus.setSelectedIndex(0);

        showPreview(null);

        table.clearSelection();

    }

}

