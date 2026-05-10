import json
import os
from llama_cpp import Llama

class LlamaWrapper:
    def __init__(self, model_path):
        self.llm = None
        self.model_path = model_path
        self.is_loaded = False
        
    def load_model(self):
        """Загружает модель llama.cpp"""
        try:
            print(f"Loading model from: {self.model_path}")
            self.llm = Llama(
                model_path=self.model_path,
                n_ctx=2048,  # Контекстное окно
                n_threads=4,  # Количество потоков
                n_gpu_layers=0,  # 0 = только CPU
                verbose=False
            )
            self.is_loaded = True
            print("Model loaded successfully")
            return True
        except Exception as e:
            print(f"Error loading model: {e}")
            return False
    
    def classify_products(self, product_names):
        """Классифицирует товары с помощью Phi-3"""
        if not self.is_loaded or self.llm is None:
            return []
        
        prompt = self._build_classification_prompt(product_names)
        
        try:
            response = self.llm(
                prompt,
                max_tokens=512,
                temperature=0.1,
                stop=["<|end|>", "\n\n"],
                echo=False
            )
            
            result_text = response['choices'][0]['text'].strip()
            return self._parse_response(result_text, product_names)
            
        except Exception as e:
            print(f"Error during inference: {e}")
            return []
    
    def _build_classification_prompt(self, product_names):
        products_text = "\n".join([f"- {name}" for name in product_names])
        
        return f"""<|user|>
Ты - классификатор товаров из чеков. Определи категории для этих товаров:

{products_text}

Доступные категории: Продукты, Одежда, Электроника, Рестораны, Транспорт, Развлечения, Здоровье, Красота, Бытовая химия, Хозтовары, Другое.

Ответь ТОЛЬКО в формате JSON без каких-либо пояснений:
{{
  "classifications": [
    {{"original_name": "название товара", "category": "категория"}}
  ]
}}
<|end|>
<|assistant|>
"""
    
    def _parse_response(self, response_text, original_names):
        """Парсит JSON ответ от модели"""
        try:
            # Ищем JSON в ответе
            start = response_text.find('{')
            end = response_text.rfind('}') + 1
            
            if start >= 0 and end > start:
                json_str = response_text[start:end]
                data = json.loads(json_str)
                
                classifications = data.get('classifications', [])
                
                # Сопоставляем с оригинальными именами
                result = []
                for orig_name in original_names:
                    classification = next(
                        (c for c in classifications if c['original_name'].lower() == orig_name.lower()),
                        {'original_name': orig_name, 'category': 'Другое'}
                    )
                    result.append(classification)
                
                return result
            else:
                print(f"No JSON found in response: {response_text}")
                return [{'original_name': name, 'category': 'Другое'} for name in original_names]
                
        except Exception as e:
            print(f"Error parsing response: {e}")
            return [{'original_name': name, 'category': 'Другое'} for name in original_names]