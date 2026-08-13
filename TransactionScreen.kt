package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionRowItem
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TransactionScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by mainViewModel.recentTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filteredTransactions = transactions.filter { txn ->
        val matchesSearch = searchQuery.isBlank() ||
                txn.title.contains(searchQuery, ignoreCase = true) ||
                txn.description.contains(searchQuery, ignoreCase = true) ||
                txn.referenceId.contains(searchQuery, ignoreCase = true)

        val matchesType = selectedFilter == null || txn.type == selectedFilter

        matchesSearch && matchesType
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Complete real-time ledger records and status history",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }

        // Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title, provider, or ID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("txn_search_input")
            )
        }

        // Category Filter Pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("ALL", fontWeight = FontWeight.Bold) }
                    )
                }

                items(TransactionType.values()) { type ->
                    FilterChip(
                        selected = selectedFilter == type,
                        onClick = { selectedFilter = type },
                        label = { Text(type.name.replace("_", " "), fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        items(filteredTransactions) { txn ->
            TransactionRowItem(transaction = txn)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
