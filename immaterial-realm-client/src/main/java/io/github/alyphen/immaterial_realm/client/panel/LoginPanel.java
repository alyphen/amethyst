package io.github.alyphen.immaterial_realm.client.panel;

import io.github.alyphen.immaterial_realm.client.ImmaterialRealmClient;
import io.github.alyphen.immaterial_realm.common.packet.serverbound.login.PacketLoginDetails;
import org.apache.commons.codec.digest.DigestUtils;

import javax.swing.*;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

public class LoginPanel extends JPanel {

    private ImmaterialRealmClient client;

    private JLabel lblUserName;
    private JTextField userNameField;
    private JLabel lblPassword;
    private JPasswordField passwordField;
    private JButton btnLogin;
    private JButton btnSignUp;
    private JLabel lblStatus;

    public LoginPanel(ImmaterialRealmClient client) {
        this.client = client;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(640, 480));
        add(Box.createVerticalGlue());
        lblUserName = new JLabel("Username: ");
        lblUserName.setAlignmentX(CENTER_ALIGNMENT);
        add(lblUserName);
        userNameField = new JTextField();
        userNameField.setPreferredSize(new Dimension(256, 24));
        userNameField.setMaximumSize(new Dimension(256, 24));
        userNameField.setAlignmentX(CENTER_ALIGNMENT);
        userNameField.addActionListener(event -> btnLogin.doClick());
        add(userNameField);
        lblUserName.setLabelFor(userNameField);
        add(Box.createVerticalStrut(16));
        lblPassword = new JLabel("Password: ");
        lblPassword.setAlignmentX(CENTER_ALIGNMENT);
        add(lblPassword);
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(256, 24));
        passwordField.setMaximumSize(new Dimension(256, 24));
        passwordField.setAlignmentX(CENTER_ALIGNMENT);
        passwordField.addActionListener(event -> btnLogin.doClick());
        add(passwordField);
        lblPassword.setLabelFor(passwordField);
        add(Box.createVerticalStrut(16));
        btnLogin = new JButton("Login");
        btnLogin.setPreferredSize(new Dimension(64, 24));
        btnLogin.setAlignmentX(CENTER_ALIGNMENT);
        btnLogin.addActionListener(event -> {
            btnLogin.setEnabled(false);
            btnSignUp.setEnabled(false);
            try {
                client.setPlayerName(userNameField.getText());
                client.getNetworkManager().sendPacket(
                        new PacketLoginDetails(
                                userNameField.getText(),
                                client.getEncryptionManager().encrypt(new String(passwordField.getPassword()), client.getNetworkManager().getServerPublicKey()),
                                false
                        )
                );
            } catch (GeneralSecurityException | UnsupportedEncodingException exception) {
                exception.printStackTrace();
            }
        });
        add(btnLogin);
        add(Box.createVerticalStrut(16));
        btnSignUp = new JButton("Sign up");
        btnSignUp.setPreferredSize(new Dimension(64, 24));
        btnSignUp.setAlignmentX(CENTER_ALIGNMENT);
        btnSignUp.addActionListener(event -> {
            btnLogin.setEnabled(false);
            btnSignUp.setEnabled(false);
            try {
                client.setPlayerName(userNameField.getText());
                client.getNetworkManager().sendPacket(
                        new PacketLoginDetails(
                                userNameField.getText(),
                                client.getEncryptionManager().encrypt(new String(passwordField.getPassword()), client.getNetworkManager().getServerPublicKey()),
                                true
                        )
                );
            } catch (GeneralSecurityException | UnsupportedEncodingException exception) {
                exception.printStackTrace();
            }
        });
        add(btnSignUp);
        add(Box.createVerticalStrut(16));
        lblStatus = new JLabel("");
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);
        add(lblStatus);
        add(Box.createVerticalGlue());
    }

    public void setStatusMessage(String message) {
        lblStatus.setText(message);
    }

    public void reEnableLoginButtons() {
        btnSignUp.setEnabled(true);
        btnLogin.setEnabled(true);
    }

}
