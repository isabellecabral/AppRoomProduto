package br.universidade.approomproduto.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.universidade.approomproduto.model.Product

@Dao
interface ProductDao {
    @Query("SELECT * FROM product")
    suspend fun getAll(): List<Product>

    @Insert
    suspend fun insertAll(vararg products: Product)

    @Query("SELECT * FROM product WHERE name LIKE :termo")
    suspend fun filtrarPorNome(termo: String): List<Product>

    @Query("DELETE FROM product")
    suspend fun apagarTodos()

    @Delete
    suspend fun remover(product: Product)

    @Update
    suspend fun atualizar(product: Product)

    @Query("SELECT * FROM product WHERE id = :id LIMIT 1")
    suspend fun obterPorId(id: Int): Product?
}
