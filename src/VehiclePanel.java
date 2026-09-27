

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
    private static final int THUMB = 150;

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final VehicleCategoryDAO categoryDAO = new VehicleCategoryDAO();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Plate", "Name", "Category", "Rate/day", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);

    private final JTextField txtPlate = new JTextField(15);
    private final JTextField txtName = new JTextField(15);
    private final JComboBox<VehicleCategory> cboCategory = new JComboBox<>();
    private final JTextField txtRate = new JTextField(10);
    private final JComboBox<String> cboStatus = new JComboBox<>(new String[]{"available", "rented", "maintenance", "inactive"});
    private final JLabel lblPreview = new JLabel();

    private BufferedImage stagedImage;      // photo waiting to be saved on Add/Update
    private String existingImageFilename;   // photo already on disk when editing
    private Vehicle selected;               // currently selected row, null = "adding new"

    public VehiclePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
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

        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        btnAdd.addActionListener(e -> addVehicle());
        btnUpdate.addActionListener(e -> updateVehicle());
        btnDelete.addActionListener(e -> deleteVehicle());
        btnClear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel();
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);
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

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Vehicle v : vehicleDAO.findAll()) {
            tableModel.addRow(new Object[]{v.id, v.plateNumber, v.name, v.categoryName, v.dailyRate, v.status});
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        selected = vehicleDAO.findById(id);

        txtPlate.setText(selected.plateNumber);
        txtName.setText(selected.name);
        txtRate.setText(selected.dailyRate.toString());
        cboStatus.setSelectedItem(selected.status);
        selectComboByCategoryId(selected.categoryId);

        stagedImage = null;
        existingImageFilename = selected.imageFilename;
        showPreview(loadExistingThumb());
    }

    private void selectComboByCategoryId(int categoryId) {
        for (int i = 0; i < cboCategory.getItemCount(); i++) {
            if (cboCategory.getItemAt(i).id == categoryId) { cboCategory.setSelectedIndex(i); return; }
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

            stagedImage = ImageUtil.resize(cropped, THUMB, THUMB); // final resize to a consistent thumbnail size
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
            stagedImage = ImageUtil.resize(dialog.result, THUMB, THUMB);
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
        lblPreview.setIcon(new ImageIcon(img));
    }

    // ---------- CRUD ----------

    private void addVehicle() {
        Vehicle v = readForm(new Vehicle());
        if (v == null) return;
        vehicleDAO.insert(v);
        if (stagedImage != null) saveStagedImage(v.id, v);
        JOptionPane.showMessageDialog(this, "Vehicle added.");
        clearForm();
        refreshTable();
    }

    private void updateVehicle() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a vehicle first."); return; }
        Vehicle v = readForm(selected);
        if (v == null) return;
        v.imageFilename = existingImageFilename; // keep unless a new photo was staged below
        if (stagedImage != null) saveStagedImage(v.id, v);
        vehicleDAO.update(v);
        JOptionPane.showMessageDialog(this, "Vehicle updated.");
        clearForm();
        refreshTable();
    }

    private void deleteVehicle() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a vehicle first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this vehicle?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        vehicleDAO.delete(selected.id);
        clearForm();
        refreshTable();
    }

    private void saveStagedImage(int vehicleId, Vehicle v) {
        try {
            String filename = vehicleId + ".png";
            ImageUtil.saveToDisk(stagedImage, IMAGE_FOLDER, filename);
            v.imageFilename = filename;
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
        VehicleCategory cat = (VehicleCategory) cboCategory.getSelectedItem();
        if (cat == null) { JOptionPane.showMessageDialog(this, "Select a category."); return null; }

        BigDecimal rate;
        try {
            rate = new BigDecimal(txtRate.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Daily rate must be a number, e.g. 1500.00");
            return null;
        }

        v.plateNumber = txtPlate.getText().trim();
        v.name        = txtName.getText().trim();
        v.categoryId  = cat.id;
        v.dailyRate   = rate;
        v.status      = (String) cboStatus.getSelectedItem();
        return v;
    }

    private void clearForm() {
        selected = null;
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
