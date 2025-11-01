package com.blockycraft.blockyminecart.storage;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BlockyMinecartDatabase {
    private final Plugin plugin;
    private Connection connection;
    private Logger logger;

    public BlockyMinecartDatabase(Plugin plugin) {
        this.plugin = plugin;
        this.logger = Logger.getLogger("Minecraft");
    }

    public void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(plugin.getDataFolder(), "blockyminecart.db");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            initTables();
        } catch (ClassNotFoundException cnfe) {
            logger.severe("[BlockyMinecart] Driver JDBC SQLite não encontrado!");
            throw new SQLException("Driver JDBC SQLite não encontrado.", cnfe);
        } catch (SQLException e) {
            logger.severe("[BlockyMinecart] Falha ao conectar/criar banco SQLite: " + e.getMessage());
            if (e.getCause() != null) {
                logger.severe("[BlockyMinecart] Causa: " + e.getCause());
            }
            throw new SQLException("Erro ao conectar ou criar o banco de dados SQLite", e);
        }
    }

    private void initTables() throws SQLException {
        Statement s = connection.createStatement();
        s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS minecarts (" +
                        "id TEXT PRIMARY KEY, " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "last_update DATETIME)"
        );
        s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS minecart_inventory (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "minecart_id TEXT, " +
                        "slot INTEGER, " +
                        "item_id INTEGER, " +
                        "amount INTEGER, " +
                        "data INTEGER, " +
                        "FOREIGN KEY(minecart_id) REFERENCES minecarts(id) ON DELETE CASCADE)"
        );
        s.close();
    }

    public void saveMinecartInventory(String minecartId, ItemStack[] items) throws SQLException {
        if (connection == null) throw new SQLException("Conexão SQLite não inicializada!");
        PreparedStatement psMinecart = connection.prepareStatement(
                "INSERT OR IGNORE INTO minecarts (id, last_update) VALUES (?, CURRENT_TIMESTAMP)"
        );
        psMinecart.setString(1, minecartId);
        psMinecart.executeUpdate();
        psMinecart.close();

        PreparedStatement psDel = connection.prepareStatement(
                "DELETE FROM minecart_inventory WHERE minecart_id = ?"
        );
        psDel.setString(1, minecartId);
        psDel.executeUpdate();
        psDel.close();

        PreparedStatement psIns = connection.prepareStatement(
                "INSERT INTO minecart_inventory (minecart_id, slot, item_id, amount, data) VALUES (?, ?, ?, ?, ?)"
        );
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item == null || item.getTypeId() == 0 || item.getAmount() == 0) continue;
            psIns.setString(1, minecartId);
            psIns.setInt(2, i);
            psIns.setInt(3, item.getTypeId());
            psIns.setInt(4, item.getAmount());
            psIns.setInt(5, item.getDurability());
            psIns.addBatch();
        }
        psIns.executeBatch();
        psIns.close();

        PreparedStatement psUpd = connection.prepareStatement(
                "UPDATE minecarts SET last_update = CURRENT_TIMESTAMP WHERE id = ?"
        );
        psUpd.setString(1, minecartId);
        psUpd.executeUpdate();
        psUpd.close();
    }

    public ItemStack[] loadMinecartInventory(String minecartId, int inventorySize) throws SQLException {
        if (connection == null) throw new SQLException("Conexão SQLite não inicializada!");
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (int i = 0; i < inventorySize; ++i) items.add(null);

        PreparedStatement ps = connection.prepareStatement(
                "SELECT slot, item_id, amount, data FROM minecart_inventory WHERE minecart_id = ?"
        );
        ps.setString(1, minecartId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int slot = rs.getInt("slot");
            int typeId = rs.getInt("item_id");
            int amount = rs.getInt("amount");
            short data = (short) rs.getInt("data");
            if (slot >= 0 && slot < inventorySize && typeId > 0 && amount > 0) {
                ItemStack stack = new ItemStack(typeId, amount, data);
                items.set(slot, stack);
            }
        }
        rs.close();
        ps.close();
        return items.toArray(new ItemStack[items.size()]);
    }

    public void deleteMinecart(String minecartId) throws SQLException {
        if (connection == null) throw new SQLException("Conexão SQLite não inicializada!");
        PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM minecarts WHERE id = ?"
        );
        ps.setString(1, minecartId);
        ps.executeUpdate();
        ps.close();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}
