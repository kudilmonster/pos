package toko.aplikasipos;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:pos_db.db";

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static String getDbUrl() {
        return DB_URL;
    }

    private static boolean tableHasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static void addColumnIfMissing(Connection conn, String tableName, String columnName, String typeDef) throws SQLException {
        if (!tableHasColumn(conn, tableName, columnName)) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + typeDef);
            }
        }
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS kategori (
                id_kategori INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_kategori TEXT UNIQUE NOT NULL
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS barang (
                id_barang INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_barang TEXT UNIQUE NOT NULL,
                kategori TEXT NOT NULL,
                harga INTEGER NOT NULL,
                stok INTEGER NOT NULL
            )
        """);

            addColumnIfMissing(conn, "barang", "stok_minimum", "INTEGER DEFAULT 5");
            addColumnIfMissing(conn, "barang", "harga_modal", "INTEGER DEFAULT 0");
            addColumnIfMissing(conn, "barang", "barcode", "TEXT");

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaksi (
                id_transaksi INTEGER PRIMARY KEY AUTOINCREMENT,
                tanggal DATETIME DEFAULT CURRENT_TIMESTAMP,
                total_harga INTEGER,
                diskon_persen INTEGER,
                pajak_persen INTEGER,
                bayar INTEGER,
                kembalian INTEGER
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS detail_transaksi (
                id_detail INTEGER PRIMARY KEY AUTOINCREMENT,
                id_transaksi INTEGER,
                nama_barang TEXT,
                harga INTEGER,
                qty INTEGER,
                subtotal INTEGER
            )
        """);

            addColumnIfMissing(conn, "transaksi", "status_transaksi", "TEXT DEFAULT 'NORMAL'");
            addColumnIfMissing(conn, "transaksi", "catatan_status", "TEXT");
            addColumnIfMissing(conn, "transaksi", "updated_by", "TEXT");
            addColumnIfMissing(conn, "detail_transaksi", "harga_modal", "INTEGER DEFAULT 0");
            addColumnIfMissing(conn, "detail_transaksi", "laba_kotor", "INTEGER DEFAULT 0");

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id_user INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS supplier (
                id_supplier INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_supplier TEXT UNIQUE NOT NULL,
                kontak TEXT,
                alamat TEXT
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS pembelian (
                id_pembelian INTEGER PRIMARY KEY AUTOINCREMENT,
                tanggal DATETIME DEFAULT CURRENT_TIMESTAMP,
                id_supplier INTEGER,
                total_beli INTEGER DEFAULT 0,
                dibuat_oleh TEXT
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS detail_pembelian (
                id_detail_beli INTEGER PRIMARY KEY AUTOINCREMENT,
                id_pembelian INTEGER,
                nama_barang TEXT,
                qty INTEGER,
                harga_beli INTEGER,
                subtotal INTEGER
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS retur_transaksi (
                id_retur INTEGER PRIMARY KEY AUTOINCREMENT,
                id_transaksi INTEGER,
                tanggal DATETIME DEFAULT CURRENT_TIMESTAMP,
                kasir TEXT,
                jenis TEXT,
                alasan TEXT,
                total_retur INTEGER DEFAULT 0
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS detail_retur (
                id_detail_retur INTEGER PRIMARY KEY AUTOINCREMENT,
                id_retur INTEGER,
                nama_barang TEXT,
                qty INTEGER,
                harga INTEGER,
                subtotal INTEGER
            )
        """);

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM kategori")) {
                if (rs.next() && rs.getInt("total") == 0) {
                    stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Makanan')");
                    stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Minuman')");
                    stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Sembako')");
                }
            }
        }
    }
}

