package sample.common;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

public class DynamoDbUtility {

	private static AmazonDynamoDB dbClient = null;
	private static DynamoDB dynamoDb = null;
	
	// テーブル名
	private static final String TEST_TABLE = "TESTTABLE";

	/**
	 * テーブル項目
	 *
	 */
	private enum AttributeName {
		UserName,
		Uuid,
		Class;
	}

	/**
	 * コンストラクタ
	 */
	public DynamoDbUtility() {

		dbClient = AmazonDynamoDBClientBuilder.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "local"))
				.build();

		dynamoDb = new DynamoDB(dbClient);
	}

	/**
	 * テーブルを作成する.
	 * 公式doc：https://docs.aws.amazon.com/ja_jp/sdk-for-java/v1/developer-guide/examples-dynamodb-tables.html
	 * 
	 */
	public static void createTable() {
		CreateTableRequest request = new CreateTableRequest()
				// 項目定義
				.withAttributeDefinitions(
						new AttributeDefinition(AttributeName.UserName.name(), ScalarAttributeType.S),
						new AttributeDefinition(AttributeName.Uuid.name(), ScalarAttributeType.S),
						new AttributeDefinition(AttributeName.Class.name(), ScalarAttributeType.S))
				// ハッシュ(プライマリキー）、レンジ（ソートキー）の指定
				.withKeySchema(
						new KeySchemaElement(AttributeName.UserName.name(), KeyType.HASH),
						new KeySchemaElement(AttributeName.Uuid.name(), KeyType.RANGE))
				/* DynamoDBの料金体系をプロビジョンドで作成する場合は設定が必要
				.withProvisionedThroughput(
						new ProvisionedThroughput().withReadCapacityUnits(10L).withWriteCapacityUnits(10L)) */

				// DynamoDBの料金体系をオンデマンドで作成する場合はこちらを使用
				.withBillingMode(BillingMode.PAY_PER_REQUEST)
				.withTableName(TEST_TABLE);
		
		dynamoDb.createTable(request);
	}
	
	/**
	 * 引数で指定したテーブルを取得する.
	 * 
	 * @param tableName テーブル名
	 * @return テーブル
	 */
	public static Table getTable(String tableName) {
		return dynamoDb.getTable(TEST_TABLE);
	}
	
	/**
	 * テーブルにデータを挿入する.
	 * 
	 */
	public static void insertRecord() {
		Item item = new Item()
				.withPrimaryKey(AttributeName.UserName.name(), "hoge")
				// 必ず一意のデータにしたいため、UUIDなんかを使うのがおすすめ				
				.withString(AttributeName.Uuid.name(), UUID.randomUUID().toString());

		getTable(TEST_TABLE).putItem(item);
	}
	
	/**
	 * クエリを実行する.
	 * 
	 * @return クエリ結果
	 */
	public static ItemCollection<QueryOutcome> query() {
		QuerySpec spec = new QuerySpec()
				// プライマリキーの指定は必須
				.withKeyConditionExpression(AttributeName.UserName.name() + "= :placeHolder1")
				// クエリの検索結果から、更に特定の値で絞りたい場合は、フィルターを使う
				.withFilterExpression(AttributeName.Class.name() + "= ::placeHolder2")
				// バリューマップの設定により、プレースホルダー（仮値）を置換する。
				.withValueMap(new ValueMap()
						.withString(":placeHolder1", "hoge")
						.withString(":placeHolder2", "Knight"));
		
		return getTable(TEST_TABLE).query(spec);
	}
	
	/**
	 * 引数で指定したテーブルに対して、スキャンを実行する.
	 * 
	 * @param tableName テーブル名
	 * @return スキャン結果
	 */
	public static ScanResult scan(String tableName) {
		ScanRequest scanRequest = new ScanRequest().withTableName(tableName);
		
		return dbClient.scan(scanRequest);
	}
	
	/**
	 * 引数で指定したテーブルに対してスキャンを実行し、全件ログ出力する.
	 * 
	 * @param tableName
	 */
	public static void scanDataOutput(String tableName) {
		ScanResult result = scan(tableName);
		// 拡張for文で回す場合
		for (Map<String, AttributeValue> item : result.getItems()) {
			System.out.println("item:" + item);
		}
		
		// イテレータで回す場合
		Iterator<Map<String, AttributeValue>> iterator = result.getItems().iterator();
		while (iterator.hasNext()) {
			Map<String, AttributeValue> item = iterator.next();

			System.out.println("item:" + item);
		}
	}

	/**
	 * クエリした結果を、全件ログ出力する.
	 * 
	 */
	public static void queryDataOutput() {
		ItemCollection<QueryOutcome> result = query();
		// 拡張for文で回す場合
		for (Item item : result) {
			System.out.println("UserName:" + item.getString(AttributeName.UserName.name()));
			System.out.println("Uuid:" + item.getString(AttributeName.Uuid.name()));
			System.out.println("Class:" + item.getString(AttributeName.Class.name()));
			
			// JSON文字列への変換もできそう.
			System.out.println("item:" + item.toJSONPretty());
		}
		
		// イテレータで回す場合
		Iterator<Item> iterator = result.iterator();
		while (iterator.hasNext()) {
			Item item = iterator.next();
			System.out.println("UserName:" + item.getString(AttributeName.UserName.name()));
			System.out.println("Uuid:" + item.getString(AttributeName.Uuid.name()));
			System.out.println("Class:" + item.getString(AttributeName.Class.name()));

			// JSON文字列への変換もできそう.
			System.out.println("item:" + item.toJSONPretty());
		}
	}
	
}
