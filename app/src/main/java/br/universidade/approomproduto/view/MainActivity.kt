package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration()
            .build()

        productDao = db.productDao()

        binding.btnInserirProduto.setOnClickListener {
            val name = binding.editProductName.text.toString().trim()
            val priceStr = binding.editProductPrice.text.toString().trim().replace(",", ".")
            val description = binding.editProductDescription.text.toString().trim()
            val qtdStr = binding.editProductQuantity.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Preencha os campos obrigatórios!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull()
            if (price == null || price <= 0.0) {
                binding.editProductPrice.error = "Informe um preço válido"
                return@setOnClickListener
            }

            val quantity = qtdStr.toIntOrNull() ?: 0
            val product = Product(
                name = name,
                price = price,
                description = description,
                quantity = quantity
            )

            lifecycleScope.launch {
                productDao.insertAll(product)
                Log.i("TAG", "Produto inserido: $name - R$$price (Qtd: $quantity)")
                Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()

                binding.editProductName.text.clear()
                binding.editProductPrice.text.clear()
                binding.editProductDescription.text.clear()
                binding.editProductQuantity.text.clear()
                binding.editProductName.requestFocus()

                mostrarNaTela(productDao.getAll())
            }
        }

        binding.btnListarProdutos.setOnClickListener {
            lifecycleScope.launch {
                val products = productDao.getAll()
                if (products.isNotEmpty()) {
                    products.forEach {
                        Log.i(
                            "TAG",
                            "ID = ${it.id}, Produto = ${it.name}, Preço = R$${it.price}, Qtd = ${it.quantity}, Descrição = ${it.description}"
                        )
                    }
                } else {
                    Log.i("TAG", "Nenhum produto encontrado.")
                }
                mostrarNaTela(products)
            }
        }

        binding.btnPesquisar.setOnClickListener {
            val busca = binding.edtPesquisa.text.toString().trim()
            lifecycleScope.launch {
                val resultado = productDao.filtrarPorNome("%$busca%")
                mostrarNaTela(resultado)
            }
        }

        binding.btnAlterar.setOnClickListener {
            val codigo = binding.edtCodigoId.text.toString().trim().toIntOrNull()
            val valorNovo = binding.edtValorNovo.text.toString().trim().replace(",", ".").toDoubleOrNull()

            if (codigo == null || valorNovo == null || valorNovo <= 0.0) {
                Toast.makeText(this, "Preencha o ID e o novo preço corretamente!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val produtoAtual = productDao.obterPorId(codigo)
                if (produtoAtual != null) {
                    val modificado = produtoAtual.copy(price = valorNovo)
                    productDao.atualizar(modificado)
                    Toast.makeText(this@MainActivity, "Produto atualizado!", Toast.LENGTH_SHORT).show()
                    binding.edtCodigoId.text.clear()
                    binding.edtValorNovo.text.clear()
                    mostrarNaTela(productDao.getAll())
                } else {
                    Toast.makeText(this@MainActivity, "Produto não encontrado!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnRemoverId.setOnClickListener {
            val codigo = binding.edtCodigoId.text.toString().trim().toIntOrNull()
            if (codigo == null) {
                Toast.makeText(this, "Digite o ID para remover!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val produtoAtual = productDao.obterPorId(codigo)
                if (produtoAtual != null) {
                    productDao.remover(produtoAtual)
                    Toast.makeText(this@MainActivity, "Item removido!", Toast.LENGTH_SHORT).show()
                    binding.edtCodigoId.text.clear()
                    mostrarNaTela(productDao.getAll())
                } else {
                    Toast.makeText(this@MainActivity, "ID inexistente!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnApagarTudo.setOnClickListener {
            lifecycleScope.launch {
                productDao.apagarTodos()
                Toast.makeText(this@MainActivity, "Todos os itens foram removidos!", Toast.LENGTH_SHORT).show()
                mostrarNaTela(emptyList())
            }
        }
    }

    private fun mostrarNaTela(products: List<Product>) {
        val qtdRegistros = products.size
        val totalPreco = products.sumOf { it.price }
        binding.txtResumo.text = String.format(
            Locale.getDefault(),
            "Total: %d produtos | Soma: R$ %.2f",
            qtdRegistros,
            totalPreco
        )

        if (products.isEmpty()) {
            binding.txtProdutos.text = "Nenhum produto encontrado."
        } else {
            binding.txtProdutos.text = products.joinToString("\n") {
                "[${it.id}] ${it.name} — R$ ${String.format(Locale.getDefault(), "%.2f", it.price)} | Est: ${it.quantity} | ${it.description}"
            }
        }
    }
}
