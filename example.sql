-- Exemple de requêtes SQL pour tester Athena SQL Executor
-- =====================================================

-- Requête 1: Compter le nombre total d'enregistrements
SELECT COUNT(*) as total_records 
FROM your_database.your_table;

-- Requête 2: Sélectionner des données avec filtrage
/* Cette requête sélectionne les données
   avec un filtre sur la date */
SELECT 
    column1,
    column2,
    column3,
    created_date
FROM your_database.your_table 
WHERE created_date >= '2024-01-01'
  AND status = 'active';

-- Requête 3: Agrégation avec GROUP BY
SELECT 
    category,
    COUNT(*) as count,
    AVG(amount) as avg_amount
FROM your_database.transactions
GROUP BY category
ORDER BY count DESC;

-- Requête 4: Jointure entre tables
SELECT 
    t.transaction_id,
    t.amount,
    c.customer_name,
    c.email
FROM your_database.transactions t
JOIN your_database.customers c 
  ON t.customer_id = c.customer_id
WHERE t.created_date >= '2024-01-01';

-- Requête 5: Sous-requête
SELECT 
    product_name,
    price,
    (SELECT AVG(price) FROM your_database.products) as avg_price
FROM your_database.products
WHERE price > (SELECT AVG(price) FROM your_database.products);
